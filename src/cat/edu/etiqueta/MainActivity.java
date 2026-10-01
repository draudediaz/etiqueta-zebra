package cat.edu.etiqueta;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String PERM_CONNECT = "android.permission.BLUETOOTH_CONNECT";
    private static final int REQ_BT = 1;

    private EditText numero;
    private EditText ample;
    private EditText alcada;
    private EditText alcadaBarres;
    private CheckBox mostrarText;
    private Spinner paper;
    private Spinner impressores;
    private Button imprimir;
    private TextView estat;

    private final List<String> adreces = new ArrayList<>();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("config", MODE_PRIVATE);
        construirPantalla();
        if (tePermis()) {
            carregarImpressores();
        } else {
            requestPermissions(new String[]{PERM_CONNECT}, REQ_BT);
        }
    }

    private boolean tePermis() {
        return Build.VERSION.SDK_INT < 31 || checkSelfPermission(PERM_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != REQ_BT) return;
        if (tePermis()) {
            carregarImpressores();
        } else {
            mostrarEstat("Cal el permís de dispositius propers (Bluetooth) per imprimir.", true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tePermis()) carregarImpressores();
    }

    private void construirPantalla() {
        int pad = dp(20);
        LinearLayout arrel = new LinearLayout(this);
        arrel.setOrientation(LinearLayout.VERTICAL);
        arrel.setPadding(pad, pad, pad, pad);

        arrel.addView(etiqueta("Número"));
        numero = new EditText(this);
        numero.setInputType(InputType.TYPE_CLASS_NUMBER);
        numero.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        numero.setImeOptions(EditorInfo.IME_ACTION_DONE);
        numero.setSingleLine(true);
        numero.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    imprimir();
                    return true;
                }
                return false;
            }
        });
        arrel.addView(numero);

        imprimir = new Button(this);
        imprimir.setText("Imprimir");
        imprimir.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        imprimir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imprimir();
            }
        });
        LinearLayout.LayoutParams lpBoto = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(64));
        lpBoto.topMargin = dp(12);
        arrel.addView(imprimir, lpBoto);

        estat = new TextView(this);
        estat.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        estat.setPadding(0, dp(12), 0, dp(24));
        arrel.addView(estat);

        arrel.addView(etiqueta("Impressora (aparellada per Bluetooth)"));
        impressores = new Spinner(this);
        arrel.addView(impressores);

        LinearLayout mides = new LinearLayout(this);
        mides.setOrientation(LinearLayout.HORIZONTAL);
        mides.setPadding(0, dp(16), 0, 0);
        ample = campMida(prefs.getInt("ampleMm", 72));
        alcada = campMida(prefs.getInt("alcadaMm", 55));
        mides.addView(columna("Ample etiqueta (mm)", ample), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        mides.addView(columna("Alçada etiqueta (mm)", alcada), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        arrel.addView(mides);

        arrel.addView(etiqueta("Alçada del codi de barres (mm)"));
        alcadaBarres = new EditText(this);
        alcadaBarres.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        alcadaBarres.setSingleLine(true);
        alcadaBarres.setText(String.format(java.util.Locale.ROOT, "%.1f", prefs.getFloat("alcadaBarresMm",
                Zpl.alcadaPerDefecte(prefs.getInt("alcadaMm", 55), prefs.getBoolean("mostrarNumero", true)))));
        arrel.addView(alcadaBarres);
        arrel.addView(etiqueta("Alçada només de les barres, en mm. El conjunt es centra a l'etiqueta. Cal espai per als marges i el número."));

        mostrarText = new CheckBox(this);
        mostrarText.setText("Mostrar el número sota el codi");
        mostrarText.setChecked(prefs.getBoolean("mostrarNumero", true));
        arrel.addView(mostrarText);

        arrel.addView(etiqueta("Tipus de paper"));
        paper = new Spinner(this);
        ArrayAdapter<String> tipus = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Zpl.TIPUS_PAPER);
        tipus.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paper.setAdapter(tipus);
        paper.setSelection(Math.min(prefs.getInt("paper", 0), Zpl.TIPUS_PAPER.length - 1));
        arrel.addView(paper);

        Button calibrar = new Button(this);
        calibrar.setText("Calibrar impressora");
        calibrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrar();
            }
        });
        LinearLayout.LayoutParams lpCalibrar = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCalibrar.topMargin = dp(16);
        arrel.addView(calibrar, lpCalibrar);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(arrel);
        setContentView(scroll);
        numero.requestFocus();
    }

    private TextView etiqueta(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return t;
    }

    private EditText campMida(int valor) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setSingleLine(true);
        e.setText(String.valueOf(valor));
        return e;
    }

    private LinearLayout columna(String titol, View camp) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(0, 0, dp(8), 0);
        c.addView(etiqueta(titol));
        c.addView(camp);
        return c;
    }

    private void carregarImpressores() {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null) {
            mostrarEstat("Aquest mòbil no té Bluetooth.", true);
            return;
        }
        if (!bt.isEnabled()) {
            mostrarEstat("Activa el Bluetooth del mòbil.", true);
        }
        List<String> noms = new ArrayList<>();
        adreces.clear();
        String desada = prefs.getString("impressora", null);
        int seleccio = -1;
        Set<BluetoothDevice> aparellats = bt.getBondedDevices();
        if (aparellats != null) {
            for (BluetoothDevice d : aparellats) {
                String nom = d.getName() != null ? d.getName() : d.getAddress();
                if (d.getAddress().equals(desada)) seleccio = adreces.size();
                else if (seleccio < 0 && semblaZebra(nom)) seleccio = adreces.size();
                noms.add(nom);
                adreces.add(d.getAddress());
            }
        }
        if (adreces.isEmpty()) {
            noms.add("Cap dispositiu aparellat");
            mostrarEstat("Aparella la ZQ320 Plus des dels ajustos de Bluetooth del mòbil.", true);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, noms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        impressores.setAdapter(adapter);
        if (seleccio >= 0) impressores.setSelection(seleccio);
    }

    private static boolean semblaZebra(String nom) {
        String n = nom.toUpperCase();
        return n.contains("ZQ") || n.contains("ZEBRA") || n.startsWith("XX");
    }

    private void imprimir() {
        final String num = numero.getText().toString().trim();
        if (num.isEmpty()) {
            mostrarEstat("Escriu un número.", true);
            return;
        }
        int mmAmple;
        int mmAlcada;
        float mmBarres;
        int tipusPaper = paper.getSelectedItemPosition();
        String zpl;
        try {
            mmAmple = Integer.parseInt(ample.getText().toString().trim());
            mmAlcada = Integer.parseInt(alcada.getText().toString().trim());
            mmBarres = Float.parseFloat(alcadaBarres.getText().toString().trim().replace(',', '.'));
            zpl = Zpl.etiqueta(num, mmAmple, mmAlcada, mmBarres, mostrarText.isChecked(), tipusPaper);
        } catch (NumberFormatException ex) {
            mostrarEstat("Introdueix mides vàlides en mm. L'ample i l'alçada de l'etiqueta han de ser enters.", true);
            return;
        } catch (IllegalArgumentException ex) {
            mostrarEstat(ex.getMessage(), true);
            return;
        }
        final String adreca = impressoraTriada();
        if (adreca == null) return;
        prefs.edit()
                .putInt("ampleMm", mmAmple)
                .putInt("alcadaMm", mmAlcada)
                .putFloat("alcadaBarresMm", mmBarres)
                .putBoolean("mostrarNumero", mostrarText.isChecked())
                .putInt("paper", tipusPaper)
                .apply();

        mostrarEstat("Imprimint " + num + "...", false);
        enviarEnSegonPla(adreca, zpl, "Imprès: " + num, true);
    }

    /** Fa que la impressora mesuri el paper (on comença i acaba cada etiqueta). */
    private void calibrar() {
        String adreca = impressoraTriada();
        if (adreca == null) return;
        int tipusPaper = paper.getSelectedItemPosition();
        prefs.edit().putInt("paper", tipusPaper).apply();
        mostrarEstat("Calibrant...", false);
        enviarEnSegonPla(adreca, Zpl.calibrar(tipusPaper), "Calibrada. Ja pots imprimir.", false);
    }

    private String impressoraTriada() {
        int pos = impressores.getSelectedItemPosition();
        if (!tePermis() || pos < 0 || pos >= adreces.size()) {
            mostrarEstat("Tria una impressora aparellada.", true);
            return null;
        }
        String adreca = adreces.get(pos);
        prefs.edit().putString("impressora", adreca).apply();
        return adreca;
    }

    private void enviarEnSegonPla(final String adreca, final String dades, final String ok, final boolean buidarNumero) {
        imprimir.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String clauZpl = "zpl_v2_" + adreca;
                String error = enviar(adreca, dades, !prefs.getBoolean(clauZpl, false));
                if (error == null) prefs.edit().putBoolean(clauZpl, true).apply();
                final String missatge = error == null ? ok : "Error: " + error;
                final boolean esError = error != null;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imprimir.setEnabled(true);
                        mostrarEstat(missatge, esError);
                        if (!esError && buidarNumero) {
                            numero.setText("");
                            numero.requestFocus();
                        }
                    }
                });
            }
        }).start();
    }

    /** Envia el ZPL per Bluetooth clàssic (SPP). Retorna null si ha anat bé, o el missatge d'error. */
    private static String enviar(String adreca, String zpl, boolean configurar) {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null || !bt.isEnabled()) return "el Bluetooth està desactivat";
        BluetoothDevice impressora = bt.getRemoteDevice(adreca);
        BluetoothSocket socket = null;
        try {
            try {
                socket = impressora.createRfcommSocketToServiceRecord(SPP);
                socket.connect();
            } catch (IOException primer) {
                tancar(socket);
                socket = impressora.createInsecureRfcommSocketToServiceRecord(SPP);
                socket.connect();
            }
            OutputStream out = socket.getOutputStream();
            if (configurar) {
                // Primer cop amb aquesta impressora: si està en mode línia (line_print) imprimiria el ZPL com a text,
                // així que la passem a ZPL. El tipus de paper va dins de cada etiqueta (^MN).
                out.write("! U1 setvar \"device.languages\" \"zpl\"\r\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
                Thread.sleep(1000);
            }
            out.write(zpl.getBytes(StandardCharsets.UTF_8));
            out.flush();
            // Les impressores mòbils Zebra poden perdre dades si es tanca la connexió massa aviat.
            Thread.sleep(1500);
            return null;
        } catch (IOException e) {
            return "no s'ha pogut connectar amb la impressora. Està encesa i a prop?";
        } catch (SecurityException e) {
            return "falta el permís de Bluetooth";
        } catch (InterruptedException e) {
            return null;
        } finally {
            tancar(socket);
        }
    }

    private static void tancar(BluetoothSocket s) {
        if (s == null) return;
        try {
            s.close();
        } catch (IOException ignored) {
        }
    }

    private void mostrarEstat(String text, boolean error) {
        estat.setText(text);
        estat.setTextColor(error ? Color.rgb(176, 0, 32) : Color.rgb(27, 94, 32));
        estat.setGravity(Gravity.START);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
