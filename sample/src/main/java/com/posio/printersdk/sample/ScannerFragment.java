package com.posio.printersdk.sample;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.posio.printersdk.PosioScanner;
import com.posio.printersdk.PrinterResult;

/**
 * Tab for the device's built-in hardware barcode scanner (infrared / QSC engine).
 *
 * <p>No on-screen camera preview: results arrive as broadcasts from the physical scan
 * button or from a software trigger, exactly like the device's native scan behaviour.
 */
public class ScannerFragment extends BaseFragment {

    private BroadcastReceiver scanReceiver;
    private TextView result;

    // In-app camera scanner (QR + 1D barcodes). Registered at fragment creation.
    private final ActivityResultLauncher<ScanOptions> cameraScan =
            registerForActivityResult(new ScanContract(), r -> {
                if (r.getContents() != null) {
                    onScan(r.getContents());
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        result = v.findViewById(R.id.scanResult);

        v.findViewById(R.id.btnCameraScan).setOnClickListener(x -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);  // QR + 1D barcodes
            options.setPrompt("Point the camera at a QR code or barcode");
            options.setBeepEnabled(true);
            options.setOrientationLocked(false);                 // follow the device orientation
            options.setCaptureActivity(DeviceOrientationCaptureActivity.class);
            cameraScan.launch(options);
        });

        v.findViewById(R.id.btnTrigger).setOnClickListener(x -> background(() -> {
            int code = printer().triggerInfraredScan(true);
            toast(code == PrinterResult.OK
                    ? "Infrared scan armed — present a barcode to the scan engine"
                    : PrinterResult.message(code));
        }));
        v.findViewById(R.id.btnStop).setOnClickListener(x -> background(() -> {
            int code = printer().triggerInfraredScan(false);
            if (code == PrinterResult.OK) {
                toast("Infrared scan stopped");
            } else if (code == PrinterResult.NOT_SUPPORTED) {
                toast("Stop isn't supported on this device — the scanner stops on its own");
            } else {
                toast(PrinterResult.message(code));
            }
        }));
    }

    @Override
    public void onStart() {
        super.onStart();
        scanReceiver = PosioScanner.registerInfraredScan(requireContext(), this::onScan);
    }

    @Override
    public void onStop() {
        if (scanReceiver != null) {
            PosioScanner.unregisterInfraredScan(requireContext(), scanReceiver);
            scanReceiver = null;
        }
        super.onStop();
    }

    private void onScan(String code) {
        String value = TextUtils.isEmpty(code) ? "(empty scan)" : code;
        if (result != null) {
            result.setText(value);
        }
        showScanResult(value);
    }

    /** Pop a modal with the scanned value so the scan loop is clearly closed. */
    private void showScanResult(String value) {
        if (getActivity() == null || requireActivity().isFinishing()) return;
        new AlertDialog.Builder(requireActivity())
                .setTitle("Scan result")
                .setMessage(value)
                .setPositiveButton("Copy", (d, w) -> {
                    ClipboardManager cb = (ClipboardManager)
                            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cb != null) {
                        cb.setPrimaryClip(ClipData.newPlainText("scan", value));
                        toast("Copied to clipboard");
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }
}
