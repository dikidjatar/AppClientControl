# FCM Notification Payload Reference

This document describes every field in `FcmNotificationPayload`, the schema used to deliver
notifications and commands from AppControl to AppClient via FCM.

All fields are serialized as a flat `data` map in the FCM message body. AppClient's `AppFcmService`
deserializes it with `kotlinx.serialization`.

---

## Identity

| Field            | Type   | Default | Description                                                                            |
|------------------|--------|---------|----------------------------------------------------------------------------------------|
| `notificationId` | String | `""`    | Stable ID for this notification. Used to cancel or update it. Auto-generated if blank. |
| `title`          | String | `""`    | Notification title.                                                                    |
| `body`           | String | `""`    | Notification body text.                                                                |

## Appearance

| Field         | Type    | Default     | Description                                                                    |
|---------------|---------|-------------|--------------------------------------------------------------------------------|
| `style`       | String  | `"default"` | Visual style: `default`, `big_text`, `image`, `inbox`, `progress`, `messaging` |
| `priority`    | String  | `"DEFAULT"` | `LOW`, `DEFAULT`, `HIGH`, `MAX`                                                |
| `icon`        | String? | null        | Small icon: drawable resource name or URL                                      |
| `largeIcon`   | String? | null        | Large icon: drawable resource name or URL                                      |
| `image`       | String? | null        | Image URL (used with style `image`)                                            |
| `color`       | String? | null        | Accent color in `#RRGGBB` format                                               |
| `ticker`      | String? | null        | Accessibility ticker text                                                      |
| `subText`     | String? | null        | Small secondary text below the app name                                        |
| `summaryText` | String? | null        | Summary text for inbox style                                                   |
| `bigText`     | String? | null        | Expanded text for `big_text` style. Falls back to `body`.                      |

## Progress Style

| Field           | Type    | Default | Description                                           |
|-----------------|---------|---------|-------------------------------------------------------|
| `progress`      | Int?    | null    | Current progress value 0–100. `null` = indeterminate. |
| `progressMax`   | Int     | 100     | Maximum progress value.                               |
| `indeterminate` | Boolean | false   | Show indeterminate spinner.                           |

## Messaging Style

The `messagingStyle` object is used when `style = "messaging"`.

```json
{
  "messagingStyle": {
    "conversationTitle": "Team Chat",
    "isGroup": true,
    "messages": [
      {
        "text": "Hello!",
        "sender": "Alice",
        "timestamp": 1700000000000
      }
    ]
  }
}
```

## Behavior

| Field        | Type    | Default | Description                                                         |
|--------------|---------|---------|---------------------------------------------------------------------|
| `autoCancel` | Boolean | true    | Dismiss notification when tapped                                    |
| `ongoing`    | Boolean | false   | Ongoing (not dismissable by swipe)                                  |
| `localOnly`  | Boolean | false   | Do not bridge to wearables                                          |
| `silent`     | Boolean | false   | No sound or vibration                                               |
| `timestamp`  | Long?   | null    | Epoch ms for the notification timestamp. Defaults to delivery time. |

## Visibility & Badge

| Field        | Type   | Default     | Description                             |
|--------------|--------|-------------|-----------------------------------------|
| `visibility` | String | `"PRIVATE"` | `PUBLIC`, `PRIVATE`, `SECRET`           |
| `badge`      | Int?   | null        | Badge count to set on the launcher icon |

## Grouping

| Field          | Type    | Default | Description                                  |
|----------------|---------|---------|----------------------------------------------|
| `group`        | String? | null    | Group key for bundling related notifications |
| `groupSummary` | Boolean | false   | This notification is the group summary       |
| `sortKey`      | String? | null    | Sort order within the group                  |

## Channel

| Field         | Type   | Default                 | Description                                           |
|---------------|--------|-------------------------|-------------------------------------------------------|
| `channelId`   | String | `"fcm_default"`         | Android notification channel ID                       |
| `channelName` | String | `"Remote Notification"` | Channel display name (used when creating the channel) |

## Actions

### Tap Action

`tapAction` defines what happens when the user taps the notification body.

```json
{
  "tapAction": {
    "action": "OPEN_URL",
    "params": {
      "url": "https://example.com"
    }
  }
}
```

### Action Buttons

`actions` is an array of up to 3 buttons shown below the notification body.

```json
{
  "actions": [
    {
      "id": "btn_reply",
      "label": "Reply",
      "action": "REPLY",
      "params": {
        "hint": "Type your message..."
      }
    },
    {
      "id": "btn_dismiss",
      "label": "Dismiss",
      "action": "DISMISS",
      "params": {}
    }
  ]
}
```

## Extra Data

| Field    | Type                | Default | Description                                                   |
|----------|---------------------|---------|---------------------------------------------------------------|
| `extras` | Map<String, String> | `{}`    | Arbitrary key-value pairs forwarded to the tap/action handler |

## Lifecycle

| Field           | Type  | Default | Description                                                                      |
|-----------------|-------|---------|----------------------------------------------------------------------------------|
| `expiresAt`     | Long? | null    | Epoch ms. If the current time is past this value, the notification is not shown. |
| `cancelAfterMs` | Long? | null    | Auto-dismiss the notification after this many milliseconds.                      |

## Special Flags

| Field             | Type         | Default | Description                                                              |
|-------------------|--------------|---------|--------------------------------------------------------------------------|
| `startMonitoring` | Boolean      | false   | Start `MonitoringService` as a side-effect of receiving this FCM message |
| `cancelIds`       | List<String> | `[]`    | Cancel these notification IDs before showing this one                    |
| `cancelOnly`      | Boolean      | false   | Only cancel `notificationId` and `cancelIds`; do not show anything       |

---