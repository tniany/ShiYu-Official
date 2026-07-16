package com.tniany.shiyu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class YandereBootReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context, Intent intent) {
    if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
      YandereMessageService.sync(context);
      NotificationKeepAliveService.sync(context);
      MomentAutoPostWorker.sync(context);
      WeixinLinkService.sync(context);
    }
  }
}
