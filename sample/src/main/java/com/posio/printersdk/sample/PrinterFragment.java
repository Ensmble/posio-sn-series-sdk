package com.posio.printersdk.sample;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.posio.printersdk.PosioPrinter;
import com.posio.printersdk.PrinterResult;
import com.posio.printerservice.print.PrintTextFormat;

import java.io.IOException;
import java.io.InputStream;

/** Tab demonstrating every printing capability of the SDK. */
public class PrinterFragment extends BaseFragment {

    /** Target raster width for printed images: ~384 px for 58 mm paper (use 576 for 80 mm). */
    private static final int PRINT_WIDTH_PX = 384;

    /** System image picker: load the chosen image, scale to the paper width, and print it. */
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;   // user cancelled
                final ContentResolver cr = requireContext().getContentResolver();
                background(() -> {
                    Bitmap bmp;
                    try {
                        bmp = loadScaledBitmap(cr, uri, PRINT_WIDTH_PX);
                    } catch (Exception e) {
                        toast("Could not read image");
                        return;
                    }
                    if (bmp == null) {
                        toast("Unsupported image (use PNG or JPEG)");
                        return;
                    }
                    int code = printer().printImage(bmp, 1, 1);   // 1=grayscale, 1=center
                    if (code == PrinterResult.OK) printer().cutPaper();
                    toast(PrinterResult.message(code));
                });
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_printer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        EditText etText = v.findViewById(R.id.etText);
        EditText etSize = v.findViewById(R.id.etSize);
        EditText etBarcode = v.findViewById(R.id.etBarcode);
        EditText etQr = v.findViewById(R.id.etQr);
        EditText etFeed = v.findViewById(R.id.etFeed);
        CheckBox cbCenter = v.findViewById(R.id.cbCenter);
        CheckBox cbBold = v.findViewById(R.id.cbBold);
        CheckBox cbUnderline = v.findViewById(R.id.cbUnderline);

        v.findViewById(R.id.btnPrintText).setOnClickListener(x ->
                run(p -> p.printText(text(etText), format(etSize, cbCenter, cbBold, cbUnderline))));

        v.findViewById(R.id.btnPrintTextWidth).setOnClickListener(x ->
                run(p -> p.printText(text(etText),
                        format(etSize, cbCenter, cbBold, cbUnderline), 240, 1)));

        v.findViewById(R.id.btnReceipt).setOnClickListener(x -> run(this::printReceipt));

        v.findViewById(R.id.btnBarcode).setOnClickListener(x ->
                run(p -> p.printBarcode(text(etBarcode), 360, 120, 2, 1, 0)));

        v.findViewById(R.id.btnQr).setOnClickListener(x ->
                run(p -> p.printQrCode(text(etQr), 300, 300, 1)));

        v.findViewById(R.id.btnImage).setOnClickListener(x ->
                run(p -> p.printImage(sampleBitmap(360, 120, "POSIO"), 0, 1)));

        v.findViewById(R.id.btnPickImage).setOnClickListener(x -> pickImage.launch("image/*"));

        v.findViewById(R.id.btnTable).setOnClickListener(x -> run(this::printTable));

        v.findViewById(R.id.btnEscpos).setOnClickListener(x -> run(this::printEscPos));

        v.findViewById(R.id.btnFeed).setOnClickListener(x ->
                run(p -> p.feedPaper(px(etFeed))));
        v.findViewById(R.id.btnBack).setOnClickListener(x ->
                run(p -> p.feedPaperBack(px(etFeed))));
        v.findViewById(R.id.btnCut).setOnClickListener(x ->
                run(PosioPrinter::cutPaper));

        v.findViewById(R.id.btnLabelDetect).setOnClickListener(x ->
                run(PosioPrinter::detectLabelAuto));
        v.findViewById(R.id.btnLabelHas).setOnClickListener(x ->
                toast("Label learning: " + printer().hasLabelLearning()));
        v.findViewById(R.id.btnLabelLocate).setOnClickListener(x ->
                run(p -> p.locateLabel(320, 24)));
        v.findViewById(R.id.btnLabelLocateAuto).setOnClickListener(x ->
                run(p -> p.locateLabelAuto(320, 24)));
        v.findViewById(R.id.btnLabelEnd).setOnClickListener(x ->
                run(PosioPrinter::endLabel));
        v.findViewById(R.id.btnLabelClear).setOnClickListener(x ->
                run(PosioPrinter::clearLabelLearning));
    }

    /** Decode an image from a content Uri, downsampled and scaled to at most {@code maxWidth} px. */
    private static Bitmap loadScaledBitmap(ContentResolver cr, Uri uri, int maxWidth) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) throw new IOException("cannot open " + uri);
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int sample = 1;
        while (bounds.outWidth > 0 && (bounds.outWidth / sample) > maxWidth * 2) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) throw new IOException("cannot open " + uri);
            bmp = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bmp == null) return null;
        if (bmp.getWidth() > maxWidth) {
            int h = Math.max(1, Math.round(bmp.getHeight() * (maxWidth / (float) bmp.getWidth())));
            bmp = Bitmap.createScaledBitmap(bmp, maxWidth, h, true);
        }
        return bmp;
    }

    private static String text(EditText e) {
        return e.getText().toString();
    }

    private static int px(EditText e) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static PrintTextFormat format(EditText size, CheckBox center, CheckBox bold, CheckBox underline) {
        PrintTextFormat fmt = PosioPrinter.textFormat();
        try {
            fmt.setTextSize(Integer.parseInt(size.getText().toString().trim()));
        } catch (NumberFormatException ignored) {
        }
        fmt.setAli(center.isChecked() ? 1 : 0);
        fmt.setStyle(bold.isChecked() ? 1 : 0);
        fmt.setUnderline(underline.isChecked());
        return fmt;
    }

    private int printReceipt(PosioPrinter p) {
        PrintTextFormat center = PosioPrinter.textFormat();
        center.setAli(1);
        center.setTextSize(36);
        PrintTextFormat normal = PosioPrinter.textFormat();

        p.printText("MY STORE", center);
        p.printText("123 Market Street", normal);
        p.feedPaper(24);
        p.printText("Coffee            3.50", normal);
        p.printText("Muffin            2.00", normal);
        p.printText("--------------------------------", normal);
        p.printText("TOTAL             5.50", normal);
        p.printQrCode("https://posio.example/r/1001", 300, 300, 1);
        return p.cutPaper();
    }

    private int printTable(PosioPrinter p) {
        PrintTextFormat left = PosioPrinter.textFormat();
        PrintTextFormat right = PosioPrinter.textFormat();
        right.setAli(2);
        return p.printTableRow(
                new String[]{"Item", "Qty", "Price"},
                new int[]{3, 1, 2},
                new PrintTextFormat[]{left, left, right});
    }

    private int printEscPos(PosioPrinter p) {
        byte[] init = {0x1B, 0x40};                 // ESC @ : initialize
        byte[] body = "ESC/POS direct print OK\n\n\n".getBytes();
        byte[] cmd = new byte[init.length + body.length];
        System.arraycopy(init, 0, cmd, 0, init.length);
        System.arraycopy(body, 0, cmd, init.length, body.length);
        return p.printEscPos(cmd);
    }
}
