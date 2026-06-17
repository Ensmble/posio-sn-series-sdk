package com.posio.printersdk.sample;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.posio.printersdk.PosioPrinter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Host activity for the Posio Printer SDK demo. Connects to the printer service once and
 * exposes it (plus a background executor) to the feature tabs.
 */
public class MainActivity extends AppCompatActivity {

    private static final String[] TAB_TITLES =
            {"Printer", "Scanner", "Display", "Cash", "Status"};

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private PosioPrinter printer;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);

        printer = new PosioPrinter(this);
        printer.connect(new PosioPrinter.ConnectionListener() {
            @Override
            public void onConnected() {
                status.setText("Connected — printer v" + printer.getPrinterVersion());
            }

            @Override
            public void onDisconnected() {
                status.setText("Not connected — is the Posio Printer Service installed?");
            }
        });

        ViewPager2 pager = findViewById(R.id.pager);
        pager.setAdapter(new MainPagerAdapter(this));
        // Keep every tab alive so background scan registration etc. stays put.
        pager.setOffscreenPageLimit(TAB_TITLES.length);

        TabLayout tabs = findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, pager,
                (tab, position) -> tab.setText(TAB_TITLES[position])).attach();

        String crash = CrashReporter.read(this);
        if (crash != null) {
            showCrashReport(crash);
        }
    }

    /** The shared printer client. Print calls must run on {@link #io()}. */
    public PosioPrinter printer() {
        return printer;
    }

    /** Single background thread for synchronous printer IPC calls. */
    public ExecutorService io() {
        return io;
    }

    /** Show the last captured crash so it can be copied or shared (no adb needed). */
    private void showCrashReport(String report) {
        new AlertDialog.Builder(this)
                .setTitle("Last crash report")
                .setMessage(report)
                .setPositiveButton("Share", (d, w) -> {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_SUBJECT, "Posio demo crash report");
                    share.putExtra(Intent.EXTRA_TEXT, report);
                    startActivity(Intent.createChooser(share, "Share crash report"));
                })
                .setNeutralButton("Copy", (d, w) -> {
                    ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cb != null) {
                        cb.setPrimaryClip(ClipData.newPlainText("crash report", report));
                        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Dismiss", (d, w) -> CrashReporter.clear(this))
                .show();
    }

    @Override
    protected void onDestroy() {
        if (printer != null) {
            printer.disconnect();
        }
        io.shutdown();
        super.onDestroy();
    }

    @NonNull
    public static String[] tabTitles() {
        return TAB_TITLES;
    }
}
