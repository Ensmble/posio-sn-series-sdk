package com.posio.printersdk.sample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Tab for the customer-display LCD controls. */
public class DisplayFragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_display, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        v.findViewById(R.id.btnLcdInit).setOnClickListener(x -> run(p -> p.configLcd(0)));
        v.findViewById(R.id.btnLcdWake).setOnClickListener(x -> run(p -> p.configLcd(1)));
        v.findViewById(R.id.btnLcdSleep).setOnClickListener(x -> run(p -> p.configLcd(2)));
        v.findViewById(R.id.btnLcdClear).setOnClickListener(x -> run(p -> p.configLcd(3)));
        v.findViewById(R.id.btnLcdReset).setOnClickListener(x -> run(p -> p.configLcd(4)));
        v.findViewById(R.id.btnLcdBitmap).setOnClickListener(x ->
                run(p -> p.showLcdBitmap(sampleBitmap(320, 160, "POSIO"))));
        v.findViewById(R.id.btnLcdLogo).setOnClickListener(x ->
                run(p -> p.setLcdLogo(sampleBitmap(320, 160, "LOGO"))));
    }
}
