# Setup Guide

This guide walks through every step required to build and run AppClientControl from source.

## Prerequisites

- **Android Studio** Panda 4 (2025.3.4 Patch 1) or later
- **JDK 17** (bundled with Android Studio)
- **Android SDK** with API 37 build tools installed
- A **Firebase project** free Spark plan is sufficient for testing
- A **Telegram account** needed to create a bot and obtain a token

---

## Step 1: Clone the Repository

```bash
git clone https://github.com/dikidjatar/AppClientControl.git
cd AppClientControl
```

---

## Step 2: Create the Firebase Project

1. Go to [console.firebase.google.com](https://console.firebase.google.com) and create a new
   project.
2. In **Build > Realtime Database**, create a database.
3. In **Build > Cloud Messaging**, enable the FCM API. Go to **Project Settings > Cloud Messaging**
   and verify the "Firebase Cloud Messaging API" is enabled.
4. In **Project Settings > Service accounts**, click **Generate new private key**. Save the
   downloaded JSON file, this is your `service_account.json`.
5. Register **two Android apps** in the Firebase project:
    - Package `com.xeg911.appclient`: download its `google-services.json`
    - Package `com.xeg911.appcontrol`: download its `google-services.json`

Place the files:

```
appclient/google-services.json
appcontrol/google-services.json
```

> These files are listed in `.gitignore` and must never be committed.

Place the service account file:

```
appcontrol/src/main/res/raw/service_account.json
```

> This file is also git-ignored. Anyone with this file can send FCM messages to every device
> registered in your Firebase project. Guard it carefully.

---

## Step 3: Configure Authentication (AppControl)

AppControl uses Firebase Authentication to protect access.

1. In the Firebase Console, go to **Build > Authentication > Sign-in method**.
2. Enable **Email/Password** And "Sing In Anonymously".
3. Create a user account for yourself in **Build > Authentication > Users**.
4. AppControl's auth screen will use these credentials.

---

## Step 4: Set Up Firebase Security Rules

In your Realtime Database, apply rules to prevent unauthorized access. A minimal starting point:

```json
{
  "rules": {
    ".read": "true",
    ".write": "true"
  }
}
```

---

## Step 5: Create a Telegram Bot (Optional but Recommended)

1. Open Telegram and start a chat with [@BotFather](https://t.me/BotFather).
2. Send `/newbot` and follow the prompts. Note the **bot token**.
3. Start a conversation with your new bot (or add it to a group), then get the **chat ID**:
   ```
   https://api.telegram.org/bot<YOUR_TOKEN>/getUpdates
   ```
   Look for `"chat":{"id":...}` in the response.
4. In AppControl, open a device's Config tab and enter the bot token and chat ID.

Once configured, AppClient will send event notifications and file transfer summaries to that
Telegram chat.

---

## Step 6: Configure the Signing Keystore

For debug builds no configuration is needed. For release builds:

1. Generate a keystore (one-time, keep it safe):
   ```bash
   keytool -genkey -v -keystore release.keystore \
     -alias my_key -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Copy `keystore.properties.example` to `keystore.properties` in the project root:
   ```bash
   cp keystore.properties.example keystore.properties
   ```

3. Edit `keystore.properties` with the actual values:
   ```properties
   storeFile=path/to/release.keystore
   storePassword=your_keystore_password
   keyAlias=my_key
   keyPassword=your_key_password
   ```

`keystore.properties` and `*.keystore` are both git-ignored.

---

## Step 7: Build and Install

```bash
# Sync Gradle dependencies
./gradlew --refresh-dependencies

# Build debug APKs
./gradlew :appclient:assembleDebug :appcontrol:assembleDebug

# Install directly to a connected device
./gradlew :appclient:installDebug  # on the monitored device
./gradlew :appcontrol:installDebug # on the controller device

# Release build (requires keystore.properties)
./gradlew :appclient:assembleRelease :appcontrol:assembleRelease
```

---

## Step 8: First Launch

**On the monitored device (AppClient):**

1. Launch AppClient.
2. Grant every permission that is requested — each permission unlocks a corresponding
   data-collection feature.
3. Disable battery optimization for AppClient (the app will prompt you).
4. AppClient will register itself with Firebase and send its FCM token.

**On the controller device (AppControl):**

1. Launch AppControl.
2. Sign in with the Firebase Authentication credentials you created in Step 3.
3. The monitored device should appear in the device list within a few seconds.
4. Tap the device to open the Device Hub.

---

## Troubleshooting

| Problem                            | Likely cause                                        | Fix                                                                |
|------------------------------------|-----------------------------------------------------|--------------------------------------------------------------------|
| Device not appearing in AppControl | `google-services.json` mismatch or RTDB not enabled | Verify both JSON files are from the same Firebase project          |
| FCM not delivering                 | Service account missing or wrong                    | Ensure `service_account.json` is in `appcontrol/src/main/res/raw/` |
| Telegram messages not arriving     | Bot token or chat ID incorrect                      | Re-check the Config tab in AppControl                              |
| Notifications not forwarded        | Notification Listener permission not granted        | Go to Settings > Notification Access and enable AppClient          |
| Location not updating              | Background location not granted                     | Grant "Allow all the time" in location settings                    |
| App killed by battery optimizer    | Battery optimization active                         | Grant "Unrestricted" battery access in Settings                    |

---