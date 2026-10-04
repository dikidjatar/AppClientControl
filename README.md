# AppClientControl

A two module Android remote-monitoring and remote-control system. **AppClient** runs on the target
device and reports data, **AppControl** is the companion controller app that sends commands and
displays everything in real time.

Communication is handled over two channels: **Firebase Realtime Database** (persistent state and
command delivery) and the **Telegram Bot API** (event notifications and file storage).

---

> **Legal & Ethical Notice**
>
> This software may only be installed and used on devices that you own or for which you have
> explicit, informed consent from the device owner. Using this software to monitor, track, or access
> data on any device without the knowledge and permission of the person who uses that device is
> illegal in most jurisdictions and a serious violation of privacy.
>
> This project is released for educational purposes only. The authors accept no liability for
> misuse. You are solely responsible for how you use this software.
> See [LEGAL_NOTICE.md](docs/LEGAL_NOTICE.md) for the full statement.

---

## Screenshots

<div style="text-align: center;">
    <img src="/screenshots/1.jpg" alt="Screenshot 1" style="width: 24%; height: auto;">
    <img src="/screenshots/2.jpg" alt="Screenshot 2" style="width: 24%; height: auto;">
    <img src="/screenshots/3.jpg" alt="Screenshot 3" style="width: 24%; height: auto;">
    <img src="/screenshots/4.jpg" alt="Screenshot 4" style="width: 24%; height: auto;">
</div>

---

## Overview

| Module       | Role                                                                                                          | Package                 |
|--------------|---------------------------------------------------------------------------------------------------------------|-------------------------|
| `appclient`  | Installed on the monitored device. Collects device data, listens for commands, and executes actions.          | `com.xeg911.appclient`  |
| `appcontrol` | Controller app. Displays device data and sends notifications/commands.                                        | `com.xeg911.appcontrol` |
| `shared`     | Pure Kotlin library. Shared data models, Firebase path constants, serialization schemas, and the rule engine. | `com.xeg911.shared`     |

The two apps communicate indirectly through **Firebase Realtime Database**. AppClient writes device
state, AppControl reads it and pushes FCM messages. Telegram is an optional but recommended
secondary channel: AppClient forwards device events there, and files are routed through Telegram Bot
Storage.

## Features at a Glance

**AppClient collects and acts on:**

- Battery level, charging state, and source.
- Hardware info (CPU, RAM, display, build fingerprint)
- Installed applications list
- Contacts
- App usage statistics (`UsageStatsManager`)
- Wallpaper
- Notifications (via `NotificationListenerService`)
- Clipboard (read and write)
- Location (foreground service, configurable interval, history, geofence rules)
- File upload from device to Telegram Storage (max 50 MB)
- File download from Telegram Storage to device (max 20 MB)
- Remote-triggered FCM notifications with 20+ action types and visual styles
- WebView overlay with JavaScript bridge
- Auto-start on device boot

**AppControl provides:**

- Device list with live online/offline status
- Device Hub with 10+ data tabs (Overview, Hardware, Apps, Contacts, Notifications, Location, Usage,
  Files, Callbacks, Config, etc.)
- Full notification composer with payload preview
- Template management
- Automation rules
- Notification filter configuration
- File transfer history
- Device configuration and required permissions management
- Telegram channel configuration

## Requirements

- Android **10** (API 29) or later on all devices
- A **Firebase project** with Realtime Database and Cloud Messaging enabled
- A **Telegram bot** (optional but strongly recommended for event notifications and file transfer)
- A **Firebase service account** JSON file placed in
  `appcontrol/src/main/res/raw/service_account.json` to send FCM HTTP v1 messages

## Project Setup

See **[docs/setup.md](docs/SETUP.md)** for the complete step-by-step guide covering:

- Firebase project creation and `google-services.json` placement
- Telegram bot creation and configuration
- Keystore generation and signing configuration
- Build and install instructions

## Documentation

| Document                                               | Description                          |
|--------------------------------------------------------|--------------------------------------|
| [docs/OVERVIEW.md](docs/OVERVIEW.md)                   | System architecture and data flow    |
| [docs/FEATURES.md](docs/FEATURES.md)                   | Detailed feature reference           |
| [docs/SETUP.md](docs/SETUP.md)                         | Project setup and build instructions |
| [docs/PAYLOAD_REFERENCE.md](docs/PAYLOAD_REFERENCE.md) | FCM notification payload schema      |
| [docs/AUTOMATION_RULES.md](docs/AUTOMATION_RULES.md)   | Automation rule engine reference     |
| [docs/LEGAL_NOTICE.md](docs/LEGAL_NOTICE.md)           | Legal and ethical usage requirements |

## Examples

The `examples/` folder contains ready-to-use payload samples:

| File                                                                                     | Description                                 |
|------------------------------------------------------------------------------------------|---------------------------------------------|
| [examples/payloads/simple_notification.json](examples/payloads/simple_notification.json) | Basic title + body notification             |
| [examples/payloads/big_text.json](examples/payloads/big_text.json)                       | Big-text expanded notification              |
| [examples/payloads/messaging_style.json](examples/payloads/messaging_style.json)         | Messaging-style notification                |
| [examples/payloads/progress_bar.json](examples/payloads/progress_bar.json)               | Progress bar notification                   |
| [examples/payloads/with_actions.json](examples/payloads/with_actions.json)               | Notification with action buttons            |
| [examples/payloads/upload_file.json](examples/payloads/upload_file.json)                 | Trigger a file upload from the device       |
| [examples/payloads/download_file.json](examples/payloads/download_file.json)             | Push a file download to the device          |
| [examples/payloads/open_webview.json](examples/payloads/open_webview.json)               | Show a remote web page in a WebView overlay |
| [examples/payloads/automation_rule.json](examples/payloads/automation_rule.json)         | Geofence-based automation rule              |

## Build

Build with Android Studio.

APKs are written to `appclient/build/outputs/apk/` and `appcontrol/build/outputs/apk/`.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md)

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
