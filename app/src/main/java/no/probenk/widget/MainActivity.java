package no.probenk.widget;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText baseUrl;
    private EditText pairCode;
    private TextView status;
    private TextView transparencyValue;
    private SeekBar transparencySeek;
    private Button connect;
    private Button test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        baseUrl = findViewById(R.id.baseUrl);
        pairCode = findViewById(R.id.pairCode);
        status = findViewById(R.id.statusText);
        transparencyValue = findViewById(R.id.transparencyValue);
        transparencySeek = findViewById(R.id.transparencySeek);
        connect = findViewById(R.id.connectButton);
        test = findViewById(R.id.testButton);

        baseUrl.setText(Prefs.baseUrl(this));

        int savedTransparency = Prefs.widgetTransparency(this);
        transparencySeek.setProgress(savedTransparency);
        updateTransparencyLabel(savedTransparency);

        transparencySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTransparencyLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Ingen handling nødvendig.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int percent = seekBar.getProgress();
                Prefs.setWidgetTransparency(MainActivity.this, percent);
                ProBenkWidgetProvider.refreshAll(MainActivity.this, false);
                status.setText("Gjennomsiktighet satt til " + percent + " %.");
            }
        });

        if (!Prefs.token(this).isEmpty()) {
            status.setText("Tilkoblet" + (Prefs.userName(this).isEmpty() ? "" : " som " + Prefs.userName(this)) + ".");
        } else {
            status.setText("Ikke tilkoblet ennå.");
        }

        connect.setOnClickListener(v -> doPair());
        test.setOnClickListener(v -> {
            status.setText("Oppdaterer …");
            ProBenkWidgetProvider.refreshAll(this, true);
            status.setText("Oppdatering sendt til widgeten.");
        });
    }

    private void updateTransparencyLabel(int percent) {
        transparencyValue.setText(percent + " %");
    }

    private void doPair() {
        String base = Prefs.normalize(baseUrl.getText().toString());
        String code = pairCode.getText().toString().trim();

        if (!base.startsWith("https://")) {
            status.setText("Portaladressen må bruke HTTPS.");
            return;
        }
        if (!code.matches("\\d{8}")) {
            status.setText("Engangskoden skal være 8 siffer.");
            return;
        }

        connect.setEnabled(false);
        status.setText("Kobler til ProBenk …");

        String device = Build.MANUFACTURER + " " + Build.MODEL;
        executor.execute(() -> {
            try {
                JSONObject json = ApiClient.pair(base, code, device.trim());
                String token = json.getString("token");
                JSONObject user = json.optJSONObject("user");
                String name = user == null ? "" : user.optString("name", "");
                Prefs.saveConnection(this, base, token, name);

                runOnUiThread(() -> {
                    connect.setEnabled(true);
                    pairCode.setText("");
                    status.setText("Tilkoblet" + (name.isEmpty() ? "" : " som " + name) + ". Legg ProBenk-widgeten på hjemskjermen.");
                    ProBenkWidgetProvider.refreshAll(this, true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    connect.setEnabled(true);
                    status.setText("Kunne ikke koble til: " + e.getMessage());
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
