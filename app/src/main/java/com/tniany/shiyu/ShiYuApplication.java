package com.tniany.shiyu;

import android.app.Application;

public final class ShiYuApplication extends Application {
  @Override public void onCreate() {
    super.onCreate();
    CrashProtection.install(this);
  }
}
