# Feature Reference

## AppClient

### Data Collection

| Feature         | Provider class                   | Firebase path                | Permission required                                   |
|-----------------|----------------------------------|------------------------------|-------------------------------------------------------|
| Device info     | `DeviceInfoProvider`             | `devices/{id}/info`          | None                                                  |
| Battery         | `BatteryInfoProvider`            | `devices/{id}/battery`       | None                                                  |
| Hardware        | `HardwareInfoProvider`           | `devices/{id}/hardware`      | None                                                  |
| Installed apps  | `InstalledAppProvider`           | `devices/{id}/apps`          | `QUERY_ALL_PACKAGES`                                  |
| Contacts        | `ContactProvider`                | `devices/{id}/contacts`      | `READ_CONTACTS`                                       |
| App usage stats | `UsageStatsProvider`             | `devices/{id}/usage`         | `PACKAGE_USAGE_STATS`                                 |
| Wallpaper       | `WallpaperProvider`              | `devices/{id}/wallpaper`     | `READ_EXTERNAL_STORAGE` (≤API 32)                     |
| Notifications   | `AppNotificationListenerService` | `devices/{id}/notifications` | Notification Listener                                 |
| Location        | `LocationProvider`               | `devices/{id}/location`      | `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION` |

### Remote Notification Actions

AppClient handles every action defined in `NotificationActionDef`. These are embedded in the
`actions[]` array (or `tapAction`) of the FCM payload.

| Action ID            | Description                                               | Required params      | Optional params                                                                                                                 |
|----------------------|-----------------------------------------------------------|----------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `NOTHING`            | No-op                                                     | —                    | —                                                                                                                               |
| `OPEN_APP`           | Bring AppClient to foreground                             | —                    | `packageName`                                                                                                                   |
| `OPEN_OTHER_APP`     | Launch another installed app                              | `packageName`        | —                                                                                                                               |
| `OPEN_URL`           | Open URL in browser                                       | `url`                | —                                                                                                                               |
| `DEEP_LINK`          | Open a deep-link URI                                      | `uri`                | —                                                                                                                               |
| `OPEN_SETTINGS`      | Open an Android Settings screen                           | `screen`             | `packageName`                                                                                                                   |
| `COPY_TO_CLIPBOARD`  | Write text to the clipboard                               | `text`               | `label`                                                                                                                         |
| `GET_CLIPBOARD`      | Read clipboard and send event                             | —                    | —                                                                                                                               |
| `REQUEST_PERMISSION` | Show a runtime permission request                         | `permission`         | `title`, `rationale`                                                                                                            |
| `DISMISS`            | Cancel the notification                                   | —                    | —                                                                                                                               |
| `REPLY`              | Show inline reply input                                   | —                    | `hint`                                                                                                                          |
| `START_COMMAND`      | Trigger an internal app command                           | `command`            | —                                                                                                                               |
| `VIEW_DETAILS`       | Open a detail screen inside AppClient                     | `detailType`         | `detailId`, `detailTitle`                                                                                                       |
| `HIDE_APP`           | Remove an app's launcher icon                             | —                    | `packageName`                                                                                                                   |
| `SHOW_APP`           | Restore a hidden app's launcher icon                      | —                    | `packageName`                                                                                                                   |
| `SHOW_WEB_PAGE`      | Display a URL or HTML in a WebView overlay                | —                    | `url`, `html`, `title`, `allowJs`, `jsBridge`, `injectedCss`, `injectedJs`, `userAgent`, `displayMode`, `clearOnExit`, and more |
| `DOWNLOAD_FILE`      | Download a file from Telegram Storage to device Downloads | `fileId`, `fileName` | `mimeType`, `fileSize`, `sha256`, `subDir`, `transferId`, `autoStart`                                                           |
| `UPLOAD_FILE`        | Pick a file on the device and upload to Telegram Storage  | `source`             | `uri`, `allowedMime`, `maxSize`, `allowMultiple`, `caption`, `transferId`                                                       |
| `SET_APP_ICON`       | Switch AppClient launcher icon                            | `iconStyle`          | `autoApply`                                                                                                                     |

### Notification Styles

The `style` field in the payload selects the visual template.

| Style value | Description                                  |
|-------------|----------------------------------------------|
| `default`   | Standard small icon + title + body           |
| `big_text`  | Expanded text body (`bigText` field)         |
| `image`     | Inline image (`image` URL)                   |
| `inbox`     | Inbox-style list                             |
| `progress`  | Progress bar (value 0–100, or indeterminate) |
| `messaging` | Conversation-style (`messagingStyle` object) |

### File Transfer Limits

| Direction                    | Limit | Imposed by                    |
|------------------------------|-------|-------------------------------|
| Upload (device → Telegram)   | 50 MB | Telegram Bot API              |
| Download (Telegram → device) | 20 MB | Telegram Bot API              |
| Files per request            | 10    | `FileTransferLimits` constant |

### Launcher Icons

AppClient supports four icon variants controlled via the `SET_APP_ICON` action or via AppControl
Configs tab:

| `iconStyle` | Alias              |
|-------------|--------------------|
| `default`   | Standard icon      |
| `mono`      | Monochrome variant |
| `shield`    | Shield-themed icon |
| `orbit`     | Orbit-themed icon  |

### Location Sharing

- Runs as a foreground service (`LocationSharingService`).
- Interval is configurable via `DeviceConfig.locationIntervalMs` (minimum 30 s, default 5 min).
- Up to 50 location history snapshots are stored in Firebase.
- Location sharing can be started/stopped remotely or by an automation rule.
- A location update request can be pushed from AppControl to get an immediate fix.

### Automation Rule Engine (Device Side)

The `RuleEngine` in the shared module evaluates `AutomationRule` objects against device state.
Device-scope rules (`RuleScope.DEVICE`) are evaluated by AppClient.

Each rule has:

- **Conditions**: one or more comparisons on fields defined in `RuleField` (battery level, charging
  state, location coordinates, connectivity type, etc.)
- **Match**: `ALL` (AND) or `ANY` (OR)
- **Actions**: `START_LOCATION_SHARING`, `STOP_LOCATION_SHARING`, `SET_CONFIG`, `START_MONITORING`,
  `REPORT_EVENT`
- **Cooldown**: minimum time between firings (default 5 min)
- **Edge-triggered**: re-fires only after condition became false in between

---

## AppControl

### Device Hub Tabs

| Tab           | Data displayed                                         | Actions available                                |
|---------------|--------------------------------------------------------|--------------------------------------------------|
| Overview      | Status, battery, connectivity, recent events           | Start monitoring, request location, ping device  |
| Hardware      | CPU, RAM, display specs, build info                    | —                                                |
| Apps          | Installed application list with icons                  | Open app remotely, hide/show launcher icon       |
| Contacts      | Contact list with search                               | —                                                |
| Notifications | Forwarded notifications                                | View details, reply via composer                 |
| Location      | Live position on map, location history, sharing status | Start/stop sharing, set interval, request update |
| Usage         | Screen time summary, per-app usage bar chart           | —                                                |
| Files         | File transfer history                                  | Download file to local device                    |
| Callbacks     | Event log from the device                              | View detail                                      |
| Config        | Per-device config (WebView URL, intervals, Telegram)   | Save config, set required permissions            |
| Permissions   | Per-permission grant status                            | Request permission remotely                      |
| Capabilities  | FCM token, notification capability definition          | Copy FCM token                                   |

### Notification Composer

The composer (`NotificationComposerScreen`) is a multisection form that builds a
`FcmNotificationPayload` and sends it via FCM HTTP v1.

**Sections:**

1. Target selection (single device or device group)
2. Basic fields (title, body, channel ID, priority, style)
3. Style-specific fields (big text, image URL, progress value, messaging)
4. Appearance (icon, large icon, color, ticker, badge, group)
5. Behavior (auto-cancel, ongoing, silent, visibility, expiry, cancelAfterMs)
6. Tap action
7. Action buttons (up to 3)
8. Extra key-value pairs
9. Advanced flags (`startMonitoring`, `cancelOnly`, `cancelIds`)

### Template System

Templates are `FcmNotificationPayload` snapshots stored in Firebase under `/notificationTemplates`.
Each template has:

- Title and description metadata
- A full payload snapshot
- Import/export as JSON

### Automation Rules (Control Side)

Control-scope rules (`RuleScope.CONTROL`) are evaluated by AppControl and can trigger actions on the
control side:

| Action              | Description                             |
|---------------------|-----------------------------------------|
| `SEND_COMMAND`      | Send a command to the monitored device  |
| `SEND_NOTIFICATION` | Send a notification to a device         |
| `SET_CONFIG`        | Update a device config value            |
| `LOCAL_ALERT`       | Show a local notification in AppControl |
| `REPORT_EVENT`      | Log an event                            |

### Notification Filter Configuration

Defines which apps' notifications are forwarded by AppClient. Each filter source has:

- Label and emoji
- Enabled/disabled flag
- Package list (or "uses default SMS app" flag)
- Noise patterns (regex/substring list to suppress noisy notifications)

### Themes

AppControl ships multiple theme presets selectable in Appearance Settings. Each preset defines a
complete Material3 color scheme for both light and dark modes.
