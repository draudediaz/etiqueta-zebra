#!/bin/sh
# Compila l'APK amb les eines de l'SDK d'Android (sense Gradle).
set -e
SDK=${ANDROID_SDK:-/usr/lib/android-sdk}
JAR=$SDK/platforms/android-23/android.jar
rm -rf build && mkdir -p build/classes
aapt package -f -M AndroidManifest.xml -I "$JAR" -F build/app.unsigned.apk
javac --release 8 -nowarn -cp "$JAR" -d build/classes $(find src -name '*.java')
dalvik-exchange --dex --output=build/classes.dex build/classes
(cd build && aapt add app.unsigned.apk classes.dex >/dev/null)
zipalign -f 4 build/app.unsigned.apk build/app.aligned.apk
[ -f etiqueta.keystore ] || keytool -genkeypair -keystore etiqueta.keystore -storepass etiqueta -keypass etiqueta \
  -alias etiqueta -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Etiqueta Zebra"
apksigner sign --ks etiqueta.keystore --ks-pass pass:etiqueta --key-pass pass:etiqueta \
  --out EtiquetaZebra.apk build/app.aligned.apk
apksigner verify -v EtiquetaZebra.apk | head -4
