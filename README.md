# Etiqueta Zebra

App Android molt senzilla: escrius un número, prems **Imprimir** i envia per Bluetooth una etiqueta ZPL amb només un codi de barres Code 128 a una Zebra ZQ320 Plus aparellada amb el mòbil.

## Instal·lar

Descarrega [`EtiquetaZebra.apk`](EtiquetaZebra.apk) al mòbil (botó de descàrrega "Download raw file"), obre-la i permet instal·lar apps de fonts desconegudes. Cal Android 6 o superior.

1. Aparella la ZQ320 Plus des dels ajustos de Bluetooth del mòbil.
2. Obre l'app i accepta el permís de *Dispositius propers*.
3. Tria la impressora (recorda l'última), ajusta ample/alçada en mm si cal (per defecte 48 × 25) i imprimeix.

## Compilar

Sense Gradle, amb les eines de l'SDK d'Android (a Ubuntu: `apt install android-sdk android-sdk-platform-23 apksigner dalvik-exchange`):

```sh
./build.sh
```

`build.sh` crea una clau de signatura `etiqueta.keystore` si no existeix (no es puja al repositori). Si canvia la clau, cal desinstal·lar la versió anterior abans d'instal·lar-ne una de nova.

## ZPL generat

Per al número `123456789012` amb etiqueta de 48 × 25 mm:

```
^XA^CI28^PW384^LL200^LH0,0^FO40,24^BY3,3,152^BCN,152,N,N,N,A^FD123456789012^FS^XZ
```
