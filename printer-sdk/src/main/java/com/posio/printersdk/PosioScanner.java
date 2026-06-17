package com.posio.printersdk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

/**
 * Helpers for the device's built-in scanners:
 * <ul>
 *   <li><b>Camera scanner</b> — opens the system barcode/QR capture screen and returns the result.</li>
 *   <li><b>Infrared (laser) scanner</b> — receives scans broadcast by the hardware trigger.</li>
 * </ul>
 *
 * <p>To trigger the infrared scanner by software instead of the hardware button, use
 * {@link PosioPrinter#triggerInfraredScan(boolean)}.
 */
public final class PosioScanner {

    private PosioScanner() {
    }

    // Built-in scanner identifiers on Posio SN-series devices.
    private static final String SCANNER_PACKAGE = "net.nyx.scanner";
    private static final String SCANNER_ACTIVITY = "net.nyx.scanner.ScannerActivity";
    private static final String INFRARED_ACTION = "com.android.NYX_QSC_DATA";
    private static final String EXTRA_SCAN_RESULT = "SCAN_RESULT";
    private static final String EXTRA_QSC = "qsc";

    /** Callback for infrared (laser) scan results. */
    public interface InfraredListener {
        void onScan(String code);
    }

    // ---- camera scanner ----------------------------------------------------

    /**
     * Open the built-in camera scanner. The result is delivered to the activity's
     * {@code onActivityResult(requestCode, resultCode, data)} — read it with {@link #parseScanResult}.
     *
     * @return false if the camera scanner app is not available on the device
     */
    public static boolean launchCameraScanner(Activity activity, int requestCode) {
        return launchCameraScanner(activity, requestCode, null);
    }

    /**
     * Open the built-in camera scanner with a custom capture-screen title.
     *
     * @return false if the camera scanner app is not available on the device
     */
    public static boolean launchCameraScanner(Activity activity, int requestCode, String title) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(SCANNER_PACKAGE, SCANNER_ACTIVITY));
        if (title != null) {
            intent.putExtra("TITLE", title);
        }
        try {
            activity.startActivityForResult(intent, requestCode);
            return true;
        } catch (Exception e) {
            // ActivityNotFoundException (scanner app missing), SecurityException (scanner
            // activity not exported to third-party apps), or any other launch failure —
            // never let it force-close the host app.
            return false;
        }
    }

    /** Extract the scanned value from the {@code onActivityResult} intent. May be null. */
    public static String parseScanResult(Intent data) {
        return data == null ? null : data.getStringExtra(EXTRA_SCAN_RESULT);
    }

    // ---- infrared (laser) scanner ------------------------------------------

    /**
     * Start receiving infrared scan results. Returns the registered {@link BroadcastReceiver};
     * pass it to {@link #unregisterInfraredScan} when you're done (e.g. in {@code onDestroy}).
     */
    public static BroadcastReceiver registerInfraredScan(Context context, InfraredListener listener) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (intent != null && INFRARED_ACTION.equals(intent.getAction())) {
                    listener.onScan(intent.getStringExtra(EXTRA_QSC));
                }
            }
        };
        IntentFilter filter = new IntentFilter(INFRARED_ACTION);
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
        return receiver;
    }

    /** Stop receiving infrared scan results. */
    public static void unregisterInfraredScan(Context context, BroadcastReceiver receiver) {
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }
    }
}
