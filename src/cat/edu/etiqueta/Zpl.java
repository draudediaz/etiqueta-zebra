package cat.edu.etiqueta;

/** Genera el ZPL d'una etiqueta amb només un codi de barres Code 128. */
public final class Zpl {
    private static final int DPMM = 8; // ZQ320 Plus: 203 dpi = 8 punts/mm

    /** Tipus de paper, en el mateix ordre que les ordres ^MN corresponents. */
    public static final String[] TIPUS_PAPER = {
            "Etiquetes amb separació (gap)",
            "Etiquetes amb marca negra al darrere",
            "Rotllo continu",
    };
    private static final String[] MN = {"^MNY", "^MNM", "^MNN"};

    private Zpl() {}

    private static String mn(int tipusPaper) {
        return MN[Math.max(0, Math.min(tipusPaper, MN.length - 1))];
    }

    /** Desa el tipus de paper i fa que la impressora el mesuri. */
    public static String calibrar(int tipusPaper) {
        return "^XA" + mn(tipusPaper) + "^JUS^XZ\r\n~JC\r\n";
    }

    public static float alcadaPerDefecte(int alcadaMm, boolean mostrarText) {
        return Math.max(4, alcadaMm - 6 - (mostrarText ? 5 : 0)) * 0.7f;
    }

    public static String etiqueta(String numero, int ampleMm, int alcadaMm, float alcadaBarresMm,
                                  boolean mostrarText, int tipusPaper) {
        if (numero == null || !numero.matches("[0-9]+")) {
            throw new IllegalArgumentException("El codi només pot contenir dígits del 0 al 9.");
        }
        if (ampleMm < 1 || ampleMm > 72 || alcadaMm < 1 || alcadaMm > 4000) {
            throw new IllegalArgumentException("L'ample ha de ser d'1 a 72 mm i l'alçada d'1 a 4000 mm.");
        }
        int ample = ampleMm * DPMM;
        int alcada = alcadaMm * DPMM;

        if (numero.length() > 42) {
            throw new IllegalArgumentException("El codi és massa llarg per imprimir-lo amb marges segurs.");
        }
        int digits = numero.length();
        int simbols = digits == 1 ? 3 : 2 + digits / 2 + (digits % 2 == 1 ? 2 : 0);
        int moduls = simbols * 11 + 13;

        int modul = 4;
        while (modul >= 2 && moduls * modul + 2 * Math.max(3 * DPMM, 10 * modul) > ample) modul--;
        if (modul < 2) {
            throw new IllegalArgumentException("El codi no cap amb marges segurs. Redueix els dígits o augmenta l'ample de l'etiqueta.");
        }
        if (mostrarText && digits * 30 > ample - 6 * DPMM) {
            throw new IllegalArgumentException("El número no cap a mida llegible. Augmenta l'ample de l'etiqueta o desactiva el número sota el codi.");
        }

        int margeVertical = 3 * DPMM;
        int text = mostrarText ? 5 * DPMM : 0;
        int maximBarres = alcada - 2 * margeVertical - text;
        if (Float.isNaN(alcadaBarresMm) || Float.isInfinite(alcadaBarresMm)
                || alcadaBarresMm < 1) {
            throw new IllegalArgumentException("Introdueix una alçada del codi vàlida, d'almenys 1 mm.");
        }
        if (alcadaBarresMm > maximBarres / (float) DPMM) {
            throw new IllegalArgumentException("Amb una etiqueta de " + alcadaMm + " mm d'alçada"
                    + (mostrarText ? " i el número visible" : "") + ", el codi pot fer com a màxim "
                    + Math.max(0, maximBarres / DPMM) + " mm. Per a un codi de "
                    + alcadaBarresMm + " mm cal una etiqueta d'almenys "
                    + (long) Math.ceil(alcadaBarresMm + 6.0 + text / DPMM)
                    + " mm d'alçada. Introdueix les mides reals del paper.");
        }
        int alcadaBarres = Math.round(alcadaBarresMm * DPMM);
        int origen = (ample - moduls * modul) / 2;
        int origenVertical = (alcada - alcadaBarres - text) / 2;
        String dades;
        if (digits == 1) {
            dades = ">:" + numero;
        } else if (digits % 2 == 0) {
            dades = ">;" + numero;
        } else {
            dades = ">;" + numero.substring(0, digits - 1) + ">6" + numero.substring(digits - 1);
        }

        return "^XA"
                + "^CI28"
                + "^PW" + ample
                + "^LL" + alcada
                + "^LH0,0"
                + mn(tipusPaper)
                + "^CF0,30"
                + "^FO" + origen + "," + origenVertical
                + "^BY" + modul + ",3," + alcadaBarres
                + "^BCN," + alcadaBarres + "," + (mostrarText ? "Y" : "N") + ",N,N,N"
                + "^FD" + dades + "^FS"
                + "^XZ\r\n";
    }
}
