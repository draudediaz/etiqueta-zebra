# Etiqueta Zebra

App Android molt senzilla: escrius un número, prems **Imprimir** i envia per Bluetooth una etiqueta ZPL amb només un codi de barres Code 128 a una Zebra ZQ320 Plus aparellada amb el mòbil.

## Instal·lar

Descarrega [`EtiquetaZebra.apk`](EtiquetaZebra.apk) al mòbil (botó de descàrrega "Download raw file"), obre-la i permet instal·lar apps de fonts desconegudes. Cal Android 6 o superior.

1. Aparella la ZQ320 Plus des dels ajustos de Bluetooth del mòbil.
2. Obre l'app i accepta el permís de *Dispositius propers*.
3. Tria la impressora (recorda l'última), ajusta ample/alçada en mm si cal (per defecte 72 × 25) i imprimeix.
4. El camp **Alçada del codi de barres (mm)** permet ajustar les barres independentment de l'etiqueta, amb decimals (coma o punt). El valor es recorda en imprimir. Inicialment és un 30% inferior a l'alçada anterior: 9,8 mm per a una etiqueta de 72 × 25 mm amb número visible, arrodonits als punts de la impressora.

## Marges i límits

- Code 128 numèric, amb selecció explícita dels subconjunts C/B per calcular l'amplada exacta, inclosos números senars i zeros inicials.
- Codi centrat amb una zona blanca a cada costat d'almenys 3 mm o 10 vegades l'amplada del mòdul, el valor més gran. Les barres s'ajusten entre 4 i 2 punts (0,5–0,25 mm a 203 dpi).
- Si el número no hi cap, es bloqueja la impressió amb un missatge. No es retallen dígits ni es redueixen les barres a un sol punt.
- Es reserven 3 mm a dalt i a baix i 5 mm addicionals per al número visible. Una alçada incompatible es rebutja, sense modificar-la silenciosament.
- Amplada màxima: 72 mm, corresponent a l'àrea imprimible d'aquesta configuració. Cal configurar l'ample real del paper i calibrar la impressora.

Els marges segueixen el criteri de zona de silenci de 10 mòduls; no impliquen una certificació legal ni GS1. Vegeu les [especificacions GS1, apartat 5.4.6.3](https://www.gs1.ch/sites/default/files/2024-02/GS1%20General%20Specifications%2001_24_0.pdf) i la [documentació Zebra de Code 128](https://docs.zebra.com/us/en/printers/software/zpl-pg/c-zpl-zpl-commands/r-zpl-bc.html). Cal validar la lectura amb la impressora, el paper i el lector reals.

## Compilar

A Windows, amb Java i l'SDK d'Android (plataforma 34 i build-tools 36.0.0):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./build.ps1
```

La versió 1.4 distribuïda des d'aquest entorn utilitza una nova clau local. Per instal·lar-la sobre la versió anterior cal desinstal·lar primer l'app antiga (es perden els ajustos desats). Conserva `etiqueta.keystore` per signar les actualitzacions següents; no es publica a GitHub.

Sense Gradle, amb les eines de l'SDK d'Android (a Ubuntu: `apt install android-sdk android-sdk-platform-23 apksigner dalvik-exchange`):

```sh
./build.sh
```

`build.sh` crea una clau de signatura `etiqueta.keystore` si no existeix (no es puja al repositori). Si canvia la clau, cal desinstal·lar la versió anterior abans d'instal·lar-ne una de nova.

## ZPL generat

Per al número `12345` amb etiqueta de 72 × 25 mm:

```
^XA^CI28^PW576^LL200^LH0,0^MNY^CF0,30^FO130,24^BY4,3,78^BCN,78,N,N,N,N^FD>;1234>65^FS^FO130,106^A0N,30,20^FB316,1,0,C,0^FD12345^FS^XZ
```
