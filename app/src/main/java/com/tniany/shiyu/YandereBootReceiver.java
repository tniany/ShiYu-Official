package com.tniany.shiyu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class YandereBootReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context, Intent intent) {
    if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
      if (MainActivity.isDataRescueRequired(context)) {
        ActiveReplyWorker.cancel(context);
        MomentAutoPostWorker.cancel(context);
        context.stopService(new Intent(context, YandereMessageService.class));
        context.stopService(new Intent(context, NotificationKeepAliveService.class));
        context.stopService(new Intent(context, WeixinLinkService.class));
        context.stopService(new Intent(context, QqBotService.class));
        context.stopService(new Intent(context, DesktopPetService.class));
        return;
      }
      YandereMessageService.sync(context);
      NotificationKeepAliveService.sync(context);
      MomentAutoPostWorker.sync(context);
      WeixinLinkService.sync(context);
      QqBotService.sync(context);
    }
  }
}
