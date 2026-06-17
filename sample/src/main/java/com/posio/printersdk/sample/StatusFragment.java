package com.posio.printersdk.sample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.posio.printersdk.PosioPrinter;

/** Tab showing printer/service info and density controls. */
public class StatusFragment extends BaseFragment {

    private TextView info;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        info = v.findViewById(R.id.info);
        EditText etDensity = v.findViewById(R.id.etDensity);

        v.findViewById(R.id.btnRefresh).setOnClickListener(x -> refresh());
        v.findViewById(R.id.btnSetDensity).setOnClickListener(x -> {
            int density;
            try {
                density = Integer.parseInt(etDensity.getText().toString().trim());
            } catch (NumberFormatException ex) {
                toast("Enter a density value");
                return;
            }
            run(p -> p.setPrinterDensity(density));
        });
    }

    private void refresh() {
        background(() -> {
            PosioPrinter p = printer();
            String text = "service version : " + p.getServiceVersion()
                    + "\nprinter version : " + p.getPrinterVersion()
                    + "\nprinter model   : " + p.getPrinterModel()
                    + "\nprinter status  : " + p.getPrinterStatus()
                    + "\nprint density   : " + p.getPrinterDensity()
                    + "\nconnected       : " + p.isConnected();
            onUi(() -> info.setText(text));
        });
    }
}
