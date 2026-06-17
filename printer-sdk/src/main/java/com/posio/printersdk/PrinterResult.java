package com.posio.printersdk;

/**
 * Result codes returned by {@link PosioPrinter} methods.
 *
 * <p>{@code 0} ({@link #OK}) always means the operation succeeded. Any negative value is an error.
 */
public final class PrinterResult {

    private PrinterResult() {
    }

    /** Operation succeeded. */
    public static final int OK = 0;

    /** The Posio Printer Service is not connected (or not installed) on the device. */
    public static final int NOT_CONNECTED = -1100;

    /** Printer cover is open. */
    public static final int COVER_OPEN = -1201;

    /** Invalid parameter. */
    public static final int PARAM_ERROR = -1202;

    /** Out of paper. */
    public static final int OUT_OF_PAPER = -1203;

    /** Printer overheated. */
    public static final int OVERHEATED = -1204;

    /** Printer is busy printing. */
    public static final int BUSY = -1206;

    /** Low battery. */
    public static final int LOW_BATTERY = -1209;

    /** Feature not supported on this device. */
    public static final int NOT_SUPPORTED = -1099;

    /** @return true if {@code code} indicates success. */
    public static boolean isSuccess(int code) {
        return code == OK;
    }

    /** @return a human-readable description of a result code. */
    public static String message(int code) {
        switch (code) {
            case OK:
                return "OK";
            case NOT_CONNECTED:
                return "printer service not connected";
            case COVER_OPEN:
                return "printer cover is open";
            case PARAM_ERROR:
                return "invalid parameter";
            case OUT_OF_PAPER:
                return "out of paper";
            case OVERHEATED:
                return "printer overheated";
            case BUSY:
                return "printer busy";
            case LOW_BATTERY:
                return "printer low battery";
            case NOT_SUPPORTED:
                return "feature not supported";
            default:
                return "printer error " + code;
        }
    }
}
