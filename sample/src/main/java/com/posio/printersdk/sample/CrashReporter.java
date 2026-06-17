package com.posio.printersdk.sample;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Debug-only crash capture for the sample app.
 *
 * <p>Records the last uncaught exception (plus device/orientation info) to a file so it can
 * be viewed, copied or shared from inside the app — handy on devices where {@code adb} is
 * not available. Install once from the {@link android.app.Application}; the previous default
 * handler is chained so the normal force-close behaviour still happens.
 */
public final class CrashReporter implements Thread.UncaughtExceptionHandler {

    private static final String FILE = "last_crash.txt";

    private final Context app;
    private final Thread.UncaughtExceptionHandler previous;

    private CrashReporter(Context context, Thread.UncaughtExceptionHandler previous) {
        this.app = context.getApplicationContext();
        this.previous = previous;
    }

    /** Start capturing uncaught exceptions. Call once, e.g. from {@code Application.onCreate}. */
    public static void install(Context context) {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new CrashReporter(context, previous));
    }

    /** @return the last stored crash report, or {@code null} if there is none. */
    public static String read(Context context) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.openFileInput(FILE)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Delete the stored crash report so it is not shown again. */
    public static void clear(Context context) {
        context.deleteFile(FILE);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            int orientation = app.getResources().getConfiguration().orientation;
            pw.println("Posio demo crash report");
            pw.println("device=" + Build.MANUFACTURER + " " + Build.MODEL);
            pw.println("android=" + Build.VERSION.RELEASE + " (sdk " + Build.VERSION.SDK_INT + ")");
            pw.println("orientation=" + (orientation == Configuration.ORIENTATION_LANDSCAPE
                    ? "landscape" : orientation == Configuration.ORIENTATION_PORTRAIT
                    ? "portrait" : "undefined"));
            pw.println("thread=" + thread.getName());
            pw.println();
            error.printStackTrace(pw);
            pw.flush();
            PrintWriter out = new PrintWriter(app.openFileOutput(FILE, Context.MODE_PRIVATE));
            out.print(sw.toString());
            out.close();
        } catch (Exception ignored) {
            // Never let crash reporting hide the original crash.
        }
        if (previous != null) {
            previous.uncaughtException(thread, error);
        }
    }
}
