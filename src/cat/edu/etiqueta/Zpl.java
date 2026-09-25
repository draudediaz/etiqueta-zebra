package cat.edu.etiqueta;

/** Genera el ZPL d'una etiqueta amb només un codi de barres Code 128. */
public final class Zpl {
    private static final int DPMM = 8; // ZQ320 Plus: 203 dpi = 8 punts/mm

    private Zpl() {}

    public static String etiqueta(String numero, int ampleMm, int alcadaMm, boolean mostrarText) {
        int ample = ampleMm * DPMM;
        int alcada = alcadaMm * DPMM;

        // Amplada aproximada del Code 128 (subconjunt C per a dígits):
        // inici + parells de dígits + (canvi de subconjunt si és senar) + control = 11 mòduls cadascun, stop = 13.
        int n = numero.length();
        int simbols = 2 + n / 2 + (n % 2 == 1 ? 2 : 0);
        int moduls = simbols * 11 + 13;

        int modul = 3;
        while (modul > 1 && moduls * modul > ample - 2 * DPMM) modul--;

        int margeVertical = 3 * DPMM;
        int text = mostrarText ? 4 * DPMM : 0;
        int alcadaBarres = Math.max(4 * DPMM, alcada - 2 * margeVertical - text);
        int x = Math.max(0, (ample - moduls * modul) / 2);

        return "^XA"
                + "^CI28"
                + "^PW" + ample
                + "^LL" + alcada
                + "^LH0,0"
                + "^FO" + x + "," + margeVertical
                + "^BY" + modul + ",3," + alcadaBarres
                + "^BCN," + alcadaBarres + "," + (mostrarText ? "Y" : "N") + ",N,N,A"
                + "^FD" + numero + "^FS"
                + "^XZ\r\n";
    }
}
