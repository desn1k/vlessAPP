# VlessApp

Android-приложение для подключения к VLESS-серверам через системный VPN и проверки
работоспособности соединения (открытие сайтов, TCP "пинги").

## Архитектура

- **Профиль-менеджер** (`data/`, `ui/ProfileListScreen.kt`, `ui/EditProfileScreen.kt`) —
  Room-база профилей, импорт по `vless://` ссылке или вручную, CRUD.
- **`vless/VlessLink.kt`** — парсинг/сериализация `vless://` share-ссылок (TLS, Reality,
  WebSocket, gRPC, XTLS flow).
- **`vless/XrayConfigFactory.kt`** — генерация JSON-конфига Xray-core под выбранный профиль.
- **`core/CoreManager.kt`** — обёртка над `libv2ray.aar` (gomobile-биндинг Xray-core из
  [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite), на этом построен v2rayNG).
- **`vpn/XrayVpnService.kt`** — системный `VpnService`: поднимает TUN-интерфейс и отдаёт его
  файловый дескриптор напрямую во встроенный "tun" inbound Xray-core (без отдельного
  tun2socks — так делает актуальный v2rayNG).
- **`test/ConnectivityTester.kt`** — реальные проверки: HTTP-запросы к тестовым сайтам и
  TCP-"пинги" (raw ICMP на Android без root недоступен, поэтому используется измерение
  времени TCP-коннекта — тот же приём, что у всех потребительских VPN-клиентов).
  Плюс прямая проверка сервера через `MeasureOutboundDelay` без поднятия VPN вообще.

## Сборка

`libv2ray.aar` нельзя получить через Maven/JitPack — это Go-код, собираемый `gomobile bind`.
Перед первой сборкой приложения выполните:

```bash
./scripts/build_libv2ray.sh
```

Скрипту нужны: Go 1.21+, Android SDK и NDK (`ANDROID_HOME`/`ANDROID_NDK_HOME`), `gomobile`.
Он клонирует AndroidLibXrayLite, собирает `libv2ray.aar` и кладёт его в `app/libs/`.

После этого:

```bash
./gradlew assembleDebug
```

## Статус

Код приложения полный (UI, профили, парсер vless-ссылок, генерация конфигов, VpnService,
тесты соединения). `libv2ray.aar` не собран в этом окружении — здесь нет Android
SDK/NDK для `gomobile bind`. Соберите его локально/в CI по инструкции выше перед
первым запуском.
