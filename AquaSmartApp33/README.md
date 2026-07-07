# AquaSmart App (Android)

Puente nativo en Kotlin hacia el portal web que **ya existe** en el ESP32.
Esta app **no** tiene dashboard, gráficas ni configuración propia: solo
detecta el dispositivo en la red y abre su página web real dentro de un
WebView integrado. Si mañana cambias el sitio web del ESP32, esta app no
necesita tocarse.

## Qué hace

1. Al abrir la app, busca el ESP32 en este orden:
   - la última IP guardada (lo normal, casi instantáneo),
   - `192.168.4.1` (modo Access Point, para primera configuración),
   - un escaneo de la subred WiFi actual del teléfono, validando en cada
     IP la respuesta de `/info` (debe contener `ssidAP` y `nombre`, propios
     del firmware de AquaSmart) para no confundirlo con otro dispositivo.
2. Si lo encuentra, guarda la IP y habilita **Abrir panel**, que carga
   `http://IP/` dentro del WebView (la misma SPA que ya sirve el ESP32).
3. **Buscar nuevamente** repite el proceso (útil si cambiaste de red).
4. **Reconfigurar WiFi** abre `http://192.168.4.1/` — para usarlo, conecta
   antes tu teléfono a la red WiFi propia del ESP32.

## Requisitos

- Android Studio (Koala o más reciente).
- JDK 17 (Android Studio ya lo trae integrado).

## Compilación automática (GitHub Actions)

El repositorio incluye `.github/workflows/android.yml`. Cada vez que subas
código a `main` (o lo lances a mano desde la pestaña **Actions**), GitHub
compila la APK por ti — no necesitas Android Studio para esto:

1. Sube el proyecto a tu repositorio.
2. Entra a la pestaña **Actions** de GitHub y espera a que termine el job
   **Android CI** (o lánzalo manualmente con "Run workflow").
3. Al terminar, entra al resumen del run y descarga el artifact
   **AquaSmart-apk** (un .zip que contiene el `.apk`).
4. Ese APK (`app-debug.apk`) ya viene firmado con la keystore de debug del
   propio SDK, así que se puede instalar directamente en un teléfono.

Nota: el workflow compila la variante **debug** porque se firma sola y
queda lista para instalar sin pasos extra. Si más adelante quieres una APK
de **release** firmada con tu propia clave (para publicarla de forma más
"oficial"), hay que añadir un `signingConfig` en `app/build.gradle.kts` y
guardar la keystore como secreto del repositorio; puedo ayudarte con eso
cuando lo necesites.

## Cómo compilar

1. Abre esta carpeta (`AquaSmartApp`) como proyecto en Android Studio.
2. Deja que sincronice Gradle (usará el `gradle-wrapper.properties`
   incluido; si Android Studio pide generar el wrapper, acepta).
3. `Build > Generate Signed Bundle / APK... > APK` (o simplemente
   `Build > Build APK(s)` para una prueba rápida sin firmar).
4. El APK queda en `app/build/outputs/apk/release/app-release.apk`
   (o `debug/app-debug.apk` si compilaste en modo debug).

## Cómo publicarlo en GitHub Releases

1. Crea un repositorio nuevo, por ejemplo `AquaSmart-App`.
2. Sube este código:
   ```bash
   git init
   git add .
   git commit -m "AquaSmart App v1.0"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/AquaSmart-App.git
   git push -u origin main
   ```
3. En GitHub, entra a **Releases > Draft a new release**.
4. Tag: `v1.0`. Título: `AquaSmart v1.0`.
5. Arrastra el archivo `AquaSmart.apk` (renombra el APK compilado, ya sea
   con Android Studio o el descargado del artifact de GitHub Actions) como
   asset de la release y publica.
6. Copia el enlace de descarga directo, con este formato:
   ```
   https://github.com/TU_USUARIO/AquaSmart-App/releases/latest/download/AquaSmart.apk
   ```
7. Pega ese enlace en el botón "Descargar aplicación" del portal web del
   ESP32 (actualmente vacío).

## Notas técnicas

- `minSdk 24`, `targetSdk 34`.
- Sin dependencias pesadas: solo AppCompat, Material, ConstraintLayout y
  coroutines para el escaneo de red en paralelo.
- `usesCleartextTraffic="true"` porque el ESP32 sirve por HTTP plano en
  la red local (no HTTPS), algo normal y esperado para este caso de uso.
- El escaneo de subred no requiere permiso de ubicación: se apoya en la
  IP DHCP del teléfono (`WifiManager`), no en el SSID/BSSID.
