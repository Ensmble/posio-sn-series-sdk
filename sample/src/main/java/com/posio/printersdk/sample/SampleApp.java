package com.posio.printersdk.sample;

import android.app.Application;

/**
 * Application entry point for the sample. Installs the {@link CrashReporter} early so crashes
 * during activity start-up are captured too.
 */
public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashReporter.install(this);
    }
}
