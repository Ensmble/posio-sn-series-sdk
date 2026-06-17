package com.posio.printersdk.sample;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

/**
 * Camera scan screen for the demo. Differs from the library's default {@code CaptureActivity}:
 * <ul>
 *   <li>orientation follows the device (manifest {@code screenOrientation="fullSensor"}),
 *       not the library's hard-locked landscape;</li>
 *   <li>shows a <b>flashlight (torch)</b> toggle;</li>
 *   <li>forces continuous autofocus, the usual reason a QR/barcode won't decode.</li>
 * </ul>
 */
public class DeviceOrientationCaptureActivity extends CaptureActivity {

    private DecoratedBarcodeView barcodeView;
    private boolean torchOn;

    @Override
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.capture_with_torch);
        barcodeView = findViewById(R.id.zxing_barcode_scanner);
        return barcodeView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Focus is the usual reason a code never decodes — force autofocus before the camera opens.
        BarcodeView bv = barcodeView.getBarcodeView();
        CameraSettings settings = bv.getCameraSettings();
        settings.setAutoFocusEnabled(true);
        settings.setContinuousFocusEnabled(true);
        bv.setCameraSettings(settings);

        Button torch = findViewById(R.id.btnTorch);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            torch.setVisibility(View.GONE);
        } else {
            torch.setOnClickListener(v -> {
                torchOn = !torchOn;
                if (torchOn) {
                    barcodeView.setTorchOn();
                    torch.setText("Torch off");
                } else {
                    barcodeView.setTorchOff();
                    torch.setText("Torch on");
                }
            });
        }
    }
}
