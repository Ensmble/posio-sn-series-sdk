package com.posio.printersdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import com.posio.printerservice.print.IPrinterService;
import com.posio.printerservice.print.PrintTextFormat;

/**
 * High-level client for the Posio thermal printer.
 *
 * <p>Wraps the binding to the on-device <b>Posio Printer Service</b> so you can print without
 * dealing with {@link ServiceConnection} or AIDL directly.
 *
 * <pre>{@code
 * PosioPrinter printer = new PosioPrinter(context);
 * printer.connect(new PosioPrinter.ConnectionListener() {
 *     public void onConnected()    { // ready to print }
 *     public void onDisconnected() { // service lost / not installed }
 * });
 *
 * // Call print methods on a BACKGROUND thread:
 * PrintTextFormat fmt = PosioPrinter.textFormat();
 * fmt.setAli(1);            // 0=left, 1=center, 2=right
 * fmt.setTextSize(32);
 * printer.printText("Hello", fmt);
 * printer.cutPaper();
 * }</pre>
 *
 * <p><b>Threading:</b> print calls are synchronous IPC. Call them off the main thread.
 * <p><b>Requirement:</b> the <b>Posio Printer Service</b> app must be installed on the device.
 */
public class PosioPrinter {

    /** Notifies when the connection to the printer service is established or lost. */
    public interface ConnectionListener {
        void onConnected();

        void onDisconnected();
    }

    private static final String SERVICE_PACKAGE = "com.posio.printerservice";
    private static final String SERVICE_ACTION = "com.posio.printerservice.IPrinterService";

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile IPrinterService service;
    private ConnectionListener listener;

    public PosioPrinter(Context context) {
        this.context = context.getApplicationContext();
    }

    /** A new, default text style you can customise and pass to {@link #printText}. */
    public static PrintTextFormat textFormat() {
        return new PrintTextFormat();
    }

    // ---- connection --------------------------------------------------------

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = IPrinterService.Stub.asInterface(binder);
            if (listener != null) main.post(listener::onConnected);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            if (listener != null) main.post(listener::onDisconnected);
        }
    };

    /**
     * Bind the printer service. {@link ConnectionListener#onConnected()} fires once ready.
     *
     * @return false if the Posio Printer Service is not installed on the device.
     */
    public boolean connect(ConnectionListener listener) {
        this.listener = listener;
        Intent intent = new Intent();
        intent.setPackage(SERVICE_PACKAGE);
        intent.setAction(SERVICE_ACTION);
        boolean ok = context.bindService(intent, conn, Context.BIND_AUTO_CREATE);
        if (!ok && listener != null) {
            main.post(listener::onDisconnected);
        }
        return ok;
    }

    /** Unbind from the printer service. */
    public void disconnect() {
        try {
            context.unbindService(conn);
        } catch (Exception ignored) {
        }
        service = null;
    }

    /** @return true if the printer service is connected and ready. */
    public boolean isConnected() {
        return service != null;
    }

    /** Direct access to the full AIDL interface, for methods not wrapped here. May be null. */
    public IPrinterService raw() {
        return service;
    }

    // ---- printing ----------------------------------------------------------

    /** Print a line of text with the default style. */
    public int printText(String text) {
        return printText(text, new PrintTextFormat());
    }

    /** Print text with a custom style. */
    public int printText(String text, PrintTextFormat format) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.printText(text, format);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /**
     * Print text constrained to a maximum block width.
     *
     * @param textWidth maximum text width, in px
     * @param align     alignment of the block within the paper: 0=left, 1=center, 2=right
     */
    public int printText(String text, PrintTextFormat format, int textWidth, int align) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.printText2(text, format, textWidth, align);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Print raw raster bitmap data (device-specific format). */
    public int printRaster(byte[] data) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.printRasterData(data);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /**
     * Print a 1D barcode.
     *
     * @param symbology 0=CODE128, 1=CODE39, 2=CODE93, 3=UPC-A, 4=UPC-E, 5=EAN13, 6=EAN8, 7=ITF, 8=CODABAR
     * @param textPosition 0=none, 1=above, 2=below, 3=both
     * @param align 0=left, 1=center, 2=right
     */
    public int printBarcode(String content, int width, int height, int textPosition, int align, int symbology) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.printBarcode(content, width, height, textPosition, align, symbology);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Print a QR code. {@code align}: 0=left, 1=center, 2=right. */
    public int printQrCode(String content, int width, int height, int align) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.printQrCode(content, width, height, align);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /**
     * Print an image.
     *
     * @param type  0=black&amp;white, 1=grayscale
     * @param align 0=left, 1=center, 2=right
     */
    public int printImage(Bitmap bitmap, int type, int align) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.printBitmap(bitmap, type, align);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Print one row of a table. {@code weights} sets each column's relative width. */
    public int printTableRow(String[] columns, int[] weights, PrintTextFormat[] formats) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.printTableText(columns, weights, formats);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Send raw ESC/POS commands (asynchronous; do not mix with other print calls). */
    public int printEscPos(byte[] commands) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            s.printEscposData(commands);
            return PrinterResult.OK;
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Advance the paper by {@code px} pixels. */
    public int feedPaper(int px) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.paperOut(px);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Move the paper backward by {@code px} pixels (on devices that support it). */
    public int feedPaperBack(int px) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.paperBack(px);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Finish the job: feed to the cutting position (and cut, on devices with a cutter). */
    public int cutPaper() {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.printEndAutoOut();
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Open the cash drawer (on devices that support one). */
    public int openCashBox() {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.openCashBox();
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Show a bitmap on the customer-display LCD (on devices that support one). */
    public int showLcdBitmap(Bitmap bitmap) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.showLcdBitmap(bitmap);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Control the customer-display LCD. {@code flag}: 0=init,1=wake,2=sleep,3=clear,4=reset. */
    public int configLcd(int flag) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.configLcd(flag);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Set the default logo image shown on the customer-display LCD (devices with one). */
    public int setLcdLogo(Bitmap bitmap) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.setLcdLogo(bitmap);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    // ---- label printing ----------------------------------------------------

    /** Start label learning by automatic label detection. */
    public int detectLabelAuto() {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.labelDetectAuto();
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** @return true if label learning has already been performed on this device. */
    public boolean hasLabelLearning() {
        IPrinterService s = service;
        if (s == null) return false;
        try {
            return s.hasLabelLearning();
        } catch (RemoteException e) {
            return false;
        }
    }

    /** Locate the next label. Must be paired with {@link #endLabel()}. */
    public int locateLabel(int labelHeight, int labelGap) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.labelLocate(labelHeight, labelGap);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Locate the next label using learned data (requires {@link #detectLabelAuto()} first). */
    public int locateLabelAuto(int labelHeight, int labelGap) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.labelLocateAuto(labelHeight, labelGap);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Finish label printing; the paper moves to the tear-off position. Pairs with {@link #locateLabel}. */
    public int endLabel() {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.labelPrintEnd();
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** Clear label-learning data. */
    public int clearLabelLearning() {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.clearLabelLearning();
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    // ---- scanner -----------------------------------------------------------

    /**
     * Trigger the built-in infrared (laser) scanner by software, on devices that have one.
     *
     * @param open true to start scanning, false to stop
     */
    public int triggerInfraredScan(boolean open) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.triggerQscScan(open ? 0 : 1);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    // ---- status ------------------------------------------------------------

    /** @return printer status; {@code 0} means ready/OK, negative values are errors. */
    public int getPrinterStatus() {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.getPrinterStatus();
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** @return the printer hardware model, or null if not connected. */
    public String getPrinterModel() {
        IPrinterService s = service;
        if (s == null) return null;
        try {
            String[] m = new String[1];
            s.getPrinterModel(m);
            return m[0];
        } catch (RemoteException e) {
            return null;
        }
    }

    /** @return the printer density, or a negative {@link PrinterResult} code on error. */
    public int getPrinterDensity() {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            int[] d = new int[1];
            int code = s.getPrinterDensity(d);
            return code == PrinterResult.OK ? d[0] : code;
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /**
     * Set the printer density. Valid values: 58mm paper 80/90/100/110/120/130,
     * 80mm paper 100/110/120/130.
     */
    public int setPrinterDensity(int density) {
        IPrinterService s = service;
        if (s == null) return PrinterResult.NOT_CONNECTED;
        try {
            return s.setPrinterDensity(density);
        } catch (RemoteException e) {
            return PrinterResult.NOT_CONNECTED;
        }
    }

    /** @return the printer firmware version, or null if not connected. */
    public String getPrinterVersion() {
        IPrinterService s = service;
        if (s == null) return null;
        try {
            String[] v = new String[1];
            s.getPrinterVersion(v);
            return v[0];
        } catch (RemoteException e) {
            return null;
        }
    }

    /** @return the printer service version, or null if not connected. */
    public String getServiceVersion() {
        IPrinterService s = service;
        if (s == null) return null;
        try {
            return s.getServiceVersion();
        } catch (RemoteException e) {
            return null;
        }
    }
}
