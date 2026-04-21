# CRYPTTVPN Android — Integration Patch

Файлы для добавления/замены в существующий `crypttvpn-android` проект. Структура отражает пакеты — копируй содержимое в соответствующие каталоги своего приложения (обычно `app/src/main/java/com/crypttvpn/...`).

## Что добавить

| Файл патча | Куда в проект |
|---|---|
| `data/remote/CrypttVpnApi.kt` | `data/remote/` |
| `data/remote/Dto.kt` | `data/remote/` |
| `data/remote/FallbackHostInterceptor.kt` | `data/remote/` |
| `data/remote/AuthInterceptor.kt` | `data/remote/` |
| `data/storage/SecureStorage.kt` | `data/storage/` |
| `di/NetworkModule.kt` | `di/` (Hilt) |
| `ui/activate/DeepLinkHandler.kt` | `ui/activate/` (используй в MainActivity) |
| `ui/qr/QrScannerScreen.kt` | `ui/qr/` |
| `work/SubscriptionCheckWorker.kt` | `work/` |
| `manifest_snippets/*.xml` | копировать блоки в `AndroidManifest.xml` |

## Зависимости (add to `app/build.gradle.kts`)

```kotlin
dependencies {
    // Retrofit + kotlinx.serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Secure storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // CameraX + ML Kit barcode (QR scanner)
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}
```

## ProGuard (`proguard-rules.pro`)

```
-keep class com.crypttvpn.data.remote.** { *; }
-keepclasseswithmembers class * { @kotlinx.serialization.Serializable <fields>; }
-keep,includedescriptorclasses class com.crypttvpn.**$$serializer { *; }
```

## Universal Link

После релиза:
1. Возьми SHA256-отпечаток подписи: `keytool -list -v -keystore release.jks | grep SHA256`
2. Вставь его в `crypttvpn-web/public/.well-known/assetlinks.json`
3. Передеплой веб.
4. Проверь: `adb shell pm verify-app-links --re-verify com.crypttvpn`
