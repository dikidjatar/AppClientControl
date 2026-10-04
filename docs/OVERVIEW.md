# System Overview

## Purpose

AppClientControl is a two-application system that lets you remotely monitor and control an Android
device from another Android device. The monitored device runs **AppClient**, the controller device
runs **AppControl**.

## Firebase Database Schema

All paths are relative to the Realtime Database root. The single source of truth is
`FirebasePaths.kt` in the shared module.

```
│
├── devices/
│   └── {deviceId}/
│       ├── info/           DeviceInfo (name, brand, Android version, etc.)
│       ├── status/         Online flag, foreground flag, monitoring flag, heartbeat
│       ├── connectivity/   Transport type, IP address, ping data
│       ├── battery/        Level, charging state, source
│       ├── hardware/       CPU, RAM, display, build
│       ├── wallpaper/      Wallpaper snapshot
│       ├── apps/           Map of installed apps
│       ├── contacts/       Map of contacts
│       ├── permissions/    Permission statuses + granted/total counts
│       ├── capabilities/   Notification capability definition
│       ├── events/         Timestamped device event log
│       ├── notifications/  Captured notifications from NotificationListenerService
│       ├── config/         Per-device config (location interval, required permissions, Telegram)
│       ├── outbox/         Sent notifications tracked by AppControl
│       ├── location/
│       │   ├── latest/     Most recent LocationSnapshot
│       │   ├── status/     Sharing active flag + parameters
│       │   ├── history/    Last 50 location snapshots
│       │   └── request/    Location request trigger written by AppControl
│       ├── usage/
│       │   ├── summary/    Total screen time, date range
│       │   └── apps/       Per-app usage entries (top 60)
│       └── ruleState/      Last evaluation result per automation rule
│
├── deviceIndex/
│   └── {deviceId}/         Lightweight mirror of info + missing-required-permissions list + FCM token
│
├── automationRules/         Global automation rules (device-scope and control-scope)
├── defaultConfig/           Default device config applied when no per-device override exists
├── notificationFilterConfig/ Notification source filter definitions
├── notificationTemplates/    Saved notification payload templates
└── notificationTemplatesMeta/ Template metadata index
```

## Data Flow

### Device State Updates (AppClient → Firebase)

1. AppClient starts `MonitoringService`, a foreground service that runs continuously.
2. On each monitoring cycle, `DeviceRepositoryImpl` collects data from providers
   (`BatteryInfoProvider`, `HardwareInfoProvider`, `InstalledAppProvider`, etc.) and writes to
   Firebase RTDB under `/devices/{deviceId}/`.
3. A lightweight copy of key fields is mirrored to `/deviceIndex/{deviceId}/` so AppControl's
   device-list screen can load quickly without reading the full device tree.
4. Firebase presence (`onDisconnect`) sets `status/online` to `false` automatically when
   connectivity is lost.

### Command Delivery (AppControl → AppClient)

1. AppControl builds a `FcmNotificationPayload` and calls `SendNotificationUseCase`.
2. The use case authenticates using the `service_account.json` (via
   `google-auth-library-oauth2-http`), then sends an FCM HTTP v1 message to the target device's FCM
   token.
3. AppClient's `AppFcmService` receives the push, deserializes the payload, and dispatches it to the
   appropriate handler (notification display, action execution, file transfer, etc.).

### Event Reporting (AppClient → Telegram)

When AppClient performs a significant action (device registered, monitoring started/stopped,
permission changed, clipboard read, file transfer completed, etc.), `DeviceEventReporter` formats a
message via `TelegramMessageFactory` and sends it to the configured Telegram chat via
`TelegramRemoteChannel`.

### File Transfer

**Upload (device → Telegram Storage)**

1. AppControl sends an `UPLOAD_FILE` action in an FCM message.
2. AppClient opens the requested file picker (MEDIA\_PICKER, PHOTO\_PICKER, FILE\_PICKER, SAF, or
   DIRECT\_URI).
3. The selected file is chunked and uploaded to Telegram using the Bot API `sendDocument` /
   `sendPhoto` endpoint (max 50 MB).
4. `TelegramStorageUploader` returns a `fileId`; AppClient reports back with a `FILE_TRANSFER` event
   containing the file metadata.
5. AppControl receives the event via Firebase, stores the record in Room, and displays it in the
   transfer history screen.

**Download (Telegram Storage → device)**

1. AppControl sends a `DOWNLOAD_FILE` action in an FCM message, including the `fileId`, `fileName`,
   and optionally `sha256`.
2. AppClient resolves the download URL via `TelegramStorageDownloader`, fetches the file, and saves
   it to `Downloads/AppClient/` (or the requested subdirectory).
3. A `FILE_TRANSFER` event with the saved path is sent back as confirmation.

## Security Model

- All communication between AppControl and AppClient passes through Firebase's security rules
  (Realtime Database) and Google's FCM infrastructure. No custom server is required.
- The `service_account.json` in AppControl is the only credential with write access to FCM, it must
  never be committed to source control.
- `google-services.json` for each module contains only the public Firebase project configuration, it
  does not grant write access by itself.
- Telegram bot tokens are stored in Firebase under `/devices/{deviceId}/config/telegram` and are
  readable only by authenticated users (enforce this in RTDB security rules).
- AppClient stores no credentials locally except the auto-refreshed FCM token.
