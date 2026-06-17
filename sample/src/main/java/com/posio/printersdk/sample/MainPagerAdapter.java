package com.posio.printersdk.sample;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/** Supplies the five feature tabs. */
public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new PrinterFragment();
            case 1: return new ScannerFragment();
            case 2: return new DisplayFragment();
            case 3: return new CashFragment();
            default: return new StatusFragment();
        }
    }

    @Override
    public int getItemCount() {
        return MainActivity.tabTitles().length;
    }
}
