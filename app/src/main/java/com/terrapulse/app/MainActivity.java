package com.terrapulse.app;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.net.*;

public class MainActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private WebView     webView;
    private ScrollView  otaView;
    private TextView    tvIp, tvConexao, tvStatus, tvProgLabel, tvFirmwareNome;
    private Button      btnSelecionarFirmware, btnEnviarFirmware;
    private ProgressBar progressBar;
    private View        statusCard;
    private LinearLayout tabDash, tabOta;
    private TextView    lblDash, lblOta;

    // ── Estado ─────────────────────────────────────────────────────────────
    private String espIp;
    private int    activeTab = 0;
    private Uri    firmwareUri  = null;
    private long   firmwareSize = 0;
    private SharedPreferences prefs;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private static final int COR_ATIVO   = 0xFF00D4FF;
    private static final int COR_INATIVO = 0xFF546E7A;

    // ── Seletor de arquivo ─────────────────────────────────────────────────
    private ActivityResultLauncher<String> filePickerLauncher;

    // ══════════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("terrapulse", MODE_PRIVATE);
        espIp = prefs.getString("ip", "192.168.15.2");

        // Referências de view
        webView               = findViewById(R.id.webView);
        otaView               = findViewById(R.id.otaView);
        tvIp                  = findViewById(R.id.tvIp);
        tvConexao             = findViewById(R.id.tvConexao);
        tvStatus              = findViewById(R.id.tvStatus);
        tvProgLabel           = findViewById(R.id.tvProgLabel);
        tvFirmwareNome        = findViewById(R.id.tvFirmwareNome);
        btnSelecionarFirmware = findViewById(R.id.btnSelecionarFirmware);
        btnEnviarFirmware     = findViewById(R.id.btnEnviarFirmware);
        progressBar           = findViewById(R.id.progressBar);
        statusCard            = findViewById(R.id.statusCard);
        tabDash               = findViewById(R.id.tabDash);
        tabOta                = findViewById(R.id.tabOta);
        lblDash               = findViewById(R.id.lblDash);
        lblOta                = findViewById(R.id.lblOta);

        tvIp.setText(espIp);
        btnEnviarFirmware.setEnabled(false);

        // ── WebView ────────────────────────────────────────────────────────
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setSupportZoom(true);
        webView.setWebViewClient(new WebViewClient() {
            private final Runnable[] retry = {null};

            private void cancelRetry() {
                if (retry[0] != null) { ui.removeCallbacks(retry[0]); retry[0] = null; }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                cancelRetry();
                ui.post(() -> tvConexao.setTextColor(Color.parseColor("#00c853")));
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                    String description, String failingUrl) {
                ui.post(() -> tvConexao.setTextColor(Color.parseColor("#d50000")));
                cancelRetry();
                retry[0] = () -> {
                    if (activeTab == 0) webView.loadUrl("http://" + espIp + "/");
                };
                ui.postDelayed(retry[0], 5000);
            }
        });

        // ── Seletor de arquivo (pasta Downloads) ───────────────────────────
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) return;
                firmwareUri  = uri;
                firmwareSize = 0;
                String nome  = "firmware.bin";
                try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                    if (c != null && c.moveToFirst()) {
                        int ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        int si = c.getColumnIndex(OpenableColumns.SIZE);
                        if (ni >= 0) nome = c.getString(ni);
                        if (si >= 0) firmwareSize = c.getLong(si);
                    }
                }
                final String nomeF = nome;
                final long   sizeF = firmwareSize;
                ui.post(() -> {
                    tvFirmwareNome.setText(nomeF + (sizeF > 0 ? "  (" + (sizeF / 1024) + " KB)" : ""));
                    tvFirmwareNome.setVisibility(View.VISIBLE);
                    btnEnviarFirmware.setEnabled(true);
                    statusCard.setVisibility(View.GONE);
                    tvProgLabel.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                });
            }
        );

        // ── Listeners ──────────────────────────────────────────────────────
        findViewById(R.id.btnEditIp).setOnClickListener(v -> showIpDialog());
        btnSelecionarFirmware.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        btnEnviarFirmware.setOnClickListener(v -> iniciarOta());
        tabDash.setOnClickListener(v -> selectTab(0));
        tabOta .setOnClickListener(v -> selectTab(1));

        selectTab(0);
    }

    // ══════════════════════════════════════════════════════════════════════
    private void selectTab(int tab) {
        activeTab = tab;
        lblDash.setTextColor(tab == 0 ? COR_ATIVO : COR_INATIVO);
        lblOta .setTextColor(tab == 1 ? COR_ATIVO : COR_INATIVO);

        if (tab == 1) {
            webView.setVisibility(View.GONE);
            otaView.setVisibility(View.VISIBLE);
        } else {
            webView.setVisibility(View.VISIBLE);
            otaView.setVisibility(View.GONE);
            webView.loadUrl("http://" + espIp + "/");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    private void showIpDialog() {
        EditText input = new EditText(this);
        input.setText(espIp);
        input.setTextColor(Color.parseColor("#e0e0e0"));
        input.setHintTextColor(Color.parseColor("#546e7a"));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.selectAll();

        new AlertDialog.Builder(this)
            .setTitle("IP do ESP32")
            .setMessage("Ex: 192.168.15.2")
            .setView(input)
            .setPositiveButton("OK", (d, w) -> {
                String ip = input.getText().toString().trim();
                if (!ip.isEmpty()) {
                    espIp = ip;
                    tvIp.setText(espIp);
                    tvConexao.setTextColor(Color.parseColor("#546e7a"));
                    prefs.edit().putString("ip", espIp).apply();
                    if (activeTab == 0) selectTab(0);
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    // ══════════════════════════════════════════════════════════════════════
    // OTA: lê da pasta Downloads e envia para o ESP32
    // ══════════════════════════════════════════════════════════════════════
    private void iniciarOta() {
        if (firmwareUri == null) return;

        btnEnviarFirmware.setEnabled(false);
        btnSelecionarFirmware.setEnabled(false);
        tvProgLabel.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        statusCard.setVisibility(View.GONE);

        String uploadUrl = "http://" + espIp + "/update";

        new Thread(() -> {
            try {
                setProgresso(5, "Lendo firmware...");
                byte[] firmware = lerArquivo(firmwareUri);

                setProgresso(50, "Leitura OK — " + (firmware.length / 1024) + " KB\nEnviando para o ESP32...");
                enviarEsp(uploadUrl, firmware);

                ui.post(() -> {
                    progressBar.setProgress(100);
                    tvProgLabel.setText("Concluído!");
                    mostrarStatus("Firmware enviado com sucesso!\nO ESP32 está reiniciando...\n\nAguarde ~10s e volte ao Dashboard.", true);
                    btnEnviarFirmware.setEnabled(true);
                    btnSelecionarFirmware.setEnabled(true);
                });

            } catch (Exception e) {
                ui.post(() -> {
                    tvProgLabel.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    mostrarStatus("Erro: " + e.getMessage(), false);
                    btnEnviarFirmware.setEnabled(true);
                    btnSelecionarFirmware.setEnabled(true);
                });
            }
        }).start();
    }

    // ── Lê o arquivo selecionado via SAF ───────────────────────────────────
    private byte[] lerArquivo(Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) throw new IOException("Nao foi possivel abrir o arquivo.");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        long lido = 0;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
            lido += n;
            final long l = lido;
            final int pct = (int)(5 + (firmwareSize > 0 ? l * 45 / firmwareSize : 20));
            setProgresso(pct, "Lendo... " + (l / 1024) + " KB");
        }
        is.close();
        return bos.toByteArray();
    }

    // ── Upload multipart para o ESP32 ──────────────────────────────────────
    private void enviarEsp(String uploadUrl, byte[] firmware) throws IOException {
        String boundary = "------TerraPulseBnd" + System.currentTimeMillis();

        // Monta as partes do multipart como bytes para calcular Content-Length exato
        byte[] part1   = ("--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"firmware\"; filename=\"firmware.bin\"\r\n"
                        + "Content-Type: application/octet-stream\r\n\r\n").getBytes("UTF-8");
        byte[] footer  = ("\r\n--" + boundary + "--\r\n").getBytes("UTF-8");
        long contentLen = part1.length + firmware.length + footer.length;

        URL url = new URL(uploadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(120_000);
        conn.setFixedLengthStreamingMode(contentLen);   // evita chunked encoding
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
        dos.write(part1);

        final int CHUNK = 4096;
        int enviado = 0;
        while (enviado < firmware.length) {
            int fim = Math.min(enviado + CHUNK, firmware.length);
            dos.write(firmware, enviado, fim - enviado);
            enviado = fim;
            final int pct = 50 + (enviado * 50 / firmware.length);
            setProgresso(pct, "Enviando... " + (enviado / 1024) + " / " + (firmware.length / 1024) + " KB");
        }
        dos.write(footer);
        dos.flush();
        dos.close();

        int code;
        try {
            code = conn.getResponseCode();
        } catch (IOException e) {
            return; // ESP32 reiniciou antes de responder — normal após flash
        }
        conn.disconnect();
        if (code != 200) throw new IOException("ESP32 retornou HTTP " + code);
    }

    private void setProgresso(int pct, String msg) {
        ui.post(() -> {
            progressBar.setProgress(pct);
            tvProgLabel.setText(msg);
        });
    }

    private void mostrarStatus(String msg, boolean sucesso) {
        tvStatus.setText(msg);
        tvStatus.setTextColor(Color.parseColor(sucesso ? "#00c853" : "#ff6d00"));
        statusCard.setVisibility(View.VISIBLE);
    }
}
