package com.tniany.shiyu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LocalModelDownloadService extends Service {
  static final String ACTION_STATE_CHANGED="com.tniany.shiyu.LOCAL_MODEL_DOWNLOAD_STATE";
  private static final String EXTRA_MODEL_ID="model_id", CHANNEL_ID="local_model_download";
  private static final int NOTIFICATION_ID=7045;
  private static volatile boolean active;
  private final ExecutorService executor=Executors.newSingleThreadExecutor();
  private SharedPreferences prefs;

  static void start(Context context,String modelId) {
    Intent intent=new Intent(context,LocalModelDownloadService.class).putExtra(EXTRA_MODEL_ID,modelId);
    if(Build.VERSION.SDK_INT>=26)context.startForegroundService(intent);else context.startService(intent);
  }
  static boolean running(Context context){return active;}

  @Override public void onCreate(){super.onCreate();prefs=getSharedPreferences(MainActivity.PREF,MODE_PRIVATE);
    prefs.edit().putBoolean("local_model_download_running",false).apply();}
  @Override public int onStartCommand(Intent intent,int flags,int startId){
    if(active)return START_NOT_STICKY;
    String id=intent==null?"":intent.getStringExtra(EXTRA_MODEL_ID);
    LocalModelRepository.ModelSpec spec=LocalModelRepository.find(id);
    if(spec==null){stopSelf();return START_NOT_STICKY;}active=true;
    update(spec,1,"准备下载","",true);startForeground(NOTIFICATION_ID,notification(spec,1,"准备下载"));
    executor.execute(()->{try{LocalModelRepository.download(this,spec,(value,label)->update(spec,value,label,"",true));
      update(spec,100,"模型已安装","",false);notifyComplete(spec,spec.name+" 已安装");
    }catch(Exception error){String detail=error.getMessage()==null?error.toString():error.getMessage();
      update(spec,0,"下载失败",detail,false);DiagnosticLog.record(this,"local_model_download","error",detail);notifyComplete(spec,"下载失败："+detail);
    }finally{active=false;stopForeground(false);stopSelf();}});return START_NOT_STICKY;
  }
  private void update(LocalModelRepository.ModelSpec spec,int progress,String label,String error,boolean running){
    prefs.edit().putBoolean("local_model_download_running",running).putString("local_model_download_id",spec.id)
        .putInt("local_model_download_progress",progress).putString("local_model_download_label",label)
        .putString("local_model_download_error",error).apply();
    if(running)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,notification(spec,progress,label));
    sendBroadcast(new Intent(ACTION_STATE_CHANGED).setPackage(getPackageName()));
  }
  private Notification notification(LocalModelRepository.ModelSpec spec,int progress,String label){
    NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    if(Build.VERSION.SDK_INT>=26){NotificationChannel channel=new NotificationChannel(CHANNEL_ID,"本地模型下载",NotificationManager.IMPORTANCE_LOW);channel.setShowBadge(false);manager.createNotificationChannel(channel);}
    Intent open=new Intent(this,ComposeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pending=PendingIntent.getActivity(this,7045,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL_ID):new Notification.Builder(this);
    return b.setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("正在下载 "+spec.name).setContentText(label)
        .setProgress(100,Math.max(0,Math.min(100,progress)),progress<=1).setContentIntent(pending).setOnlyAlertOnce(true).setOngoing(true).build();
  }
  private void notifyComplete(LocalModelRepository.ModelSpec spec,String text){
    Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL_ID):new Notification.Builder(this);
    ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,b.setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(spec.name).setContentText(text).setAutoCancel(true).build());
  }
  @Override public void onDestroy(){active=false;executor.shutdownNow();super.onDestroy();}
  @Override public IBinder onBind(Intent intent){return null;}
}
