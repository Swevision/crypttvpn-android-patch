# CRYPTTVPN Android — Account Screen Patch

Compose-экран ЛК для приложения. Полностью повторяет веб-кабинет:
имя/email, дата окончания подписки, активное устройство (модель + OS),
ключ активации с кнопкой копирования, история платежей, audit log.

## Файлы

| Файл | Куда копировать |
|---|---|
| `data/remote/AccountDto.kt` | `app/src/main/java/com/crypttvpn/data/remote/` |
| `data/remote/AccountApi.kt` | `app/src/main/java/com/crypttvpn/data/remote/` |
| `data/AccountRepository.kt` | `app/src/main/java/com/crypttvpn/data/` |
| `di/AccountModule.kt` | `app/src/main/java/com/crypttvpn/di/` |
| `ui/account/AccountViewModel.kt` | `app/src/main/java/com/crypttvpn/ui/account/` |
| `ui/account/AccountScreen.kt` | `app/src/main/java/com/crypttvpn/ui/account/` |

## Зависимости

Уже должны быть в проекте (см. предыдущие патчи):
- Retrofit 2.11 + kotlinx-serialization-converter
- OkHttp 4.12 + AuthInterceptor (подмешивает Bearer JWT)
- Hilt 2.51+ + hilt-compose 1.2
- Compose Material3
- `kotlinx-coroutines-android`

Если ещё нет:
```kotlin
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
implementation("androidx.compose.material:material-icons-extended:1.6.8")
```

## Подключение в навигацию

```kotlin
NavHost(navController, startDestination = "vpn") {
    composable("vpn") { VpnScreen() }
    composable("account") { AccountScreen() }
    composable("settings") { SettingsScreen() }
}
```

И в нижней навигации добавь третью кнопку «ЛК» — `navController.navigate("account")`.

## Что делает

1. При входе вызывает `GET /v1/app/account` с device-JWT (AuthInterceptor подмешает Bearer автоматически).
2. Параллельно `GET /v1/app/status` — если выставлен баннер на бэке, рисует его сверху.
3. Рендерит секции:
   - `// ОПЛАЧЕНО ДО` — дата + дни до конца. Если ключ не активирован — «Ключ готов».
   - `// АКТИВНОЕ УСТРОЙСТВО` — модель из `Build.MODEL` + ОС из `Build.VERSION.RELEASE` (берётся из device_info, который шлётся при активации).
   - `// КЛЮЧ АКТИВАЦИИ` — сам ключ + бейдж статуса + кнопка копирования.
   - `// ИСТОРИЯ ПЛАТЕЖЕЙ` — последние 10 платежей (сумма, дата, статус).
   - `// АКТИВНОСТЬ` — последние 20 событий audit log (генерация ключа, активация, платёж, revoke).

## Что нужно от активации

При активации (`POST /v1/activate`) шли в `device_info`:

```kotlin
DeviceInfo(
    model = Build.MODEL,                     // "Pixel 8" или "M2102J20SG"
    manufacturer = Build.MANUFACTURER,       // "Google", "Xiaomi"
    osVersion = Build.VERSION.RELEASE,       // "14"
    appVersion = BuildConfig.VERSION_NAME,
    platform = "android",
)
```

Это подтянется в ЛК как «Активное устройство».

## Цвета

Хардкодом в файле (внизу `AccountScreen.kt`):
- `#0A0A0A` — фон
- `#141414` — карточки
- `#E63946` — primary (красный)
- `#06D6A0` — success
- `#8B8B8B` — text-secondary

Если у проекта уже есть `MaterialTheme` с теми же цветами — можно заменить на `MaterialTheme.colorScheme.*`.
