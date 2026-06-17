package com.posio.printersdk.sample;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.posio.printersdk.PosioPrinter;
import com.posio.printersdk.PrinterResult;

/** Shared helpers for the feature tabs: access to the printer, background execution, toasts. */
public abstract class BaseFragment extends Fragment {

    /** A printer operation that returns a {@link PrinterResult} code. */
    public interface PrinterJob {
        int run(PosioPrinter printer) throws Exception;
    }

    protected PosioPrinter printer() {
        return ((MainActivity) requireActivity()).printer();
    }

    /** Run a printer call on the background thread, then toast its result code. */
    protected void run(PrinterJob job) {
        MainActivity host = (MainActivity) requireActivity();
        PosioPrinter printer = host.printer();
        host.io().execute(() -> {
            int code;
            try {
                code = job.run(printer);
            } catch (Exception e) {
                code = PrinterResult.NOT_CONNECTED;
            }
            toast(PrinterResult.message(code));
        });
    }

    protected void toast(String message) {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
    }

    /** Run work on the shared background thread (printer IPC must not touch the UI thread). */
    protected void background(Runnable work) {
        ((MainActivity) requireActivity()).io().execute(work);
    }

    /** Post work back to the UI thread (safe no-op if the fragment is detached). */
    protected void onUi(Runnable work) {
        if (getActivity() != null) {
            requireActivity().runOnUiThread(work);
        }
    }

    /** Build a simple black-on-white bitmap to demonstrate image printing / the LCD. */
    protected static Bitmap sampleBitmap(int width, int height, String text) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(height * 0.5f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float y = height / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, width / 2f, y, paint);
        return bmp;
    }
}
