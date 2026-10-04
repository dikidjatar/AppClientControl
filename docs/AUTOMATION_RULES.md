# Automation Rules

The automation rule engine (`RuleEngine` in the shared module) lets you define condition-based
triggers that run automatically without manual intervention.

## Rule Anatomy

```
AutomationRule {
  id              — unique string, auto-generated
  name            — human label shown in the UI
  enabled         — toggle without deleting the rule
  scope           — DEVICE or CONTROL
  deviceIds       — target device IDs; empty = all devices
  match           — ALL (AND logic) or ANY (OR logic) across conditions
  conditions      — list of RuleCondition objects
  actions         — list of RuleAction objects
  cooldownMs      — minimum ms between two firings (default: 5 min)
  edgeTriggered   — re-fires only after conditions became false first
  createdAt       — epoch ms
  updatedAt       — epoch ms
}
```

## Scopes

| Scope     | Where evaluated                     | Available actions                                                                                   |
|-----------|-------------------------------------|-----------------------------------------------------------------------------------------------------|
| `DEVICE`  | AppClient on the monitored device   | `START_LOCATION_SHARING`, `STOP_LOCATION_SHARING`, `SET_CONFIG`, `START_MONITORING`, `REPORT_EVENT` |
| `CONTROL` | AppControl on the controller device | `SEND_COMMAND`, `SEND_NOTIFICATION`, `SET_CONFIG`, `LOCAL_ALERT`, `REPORT_EVENT`                    |

## Condition Fields

Fields are dot-separated paths referencing the Firebase device state. Common fields:

| Field key                    | Type    | Example value |
|------------------------------|---------|---------------|
| `battery.level`              | Int     | `15`          |
| `battery.charging`           | Boolean | `false`       |
| `battery.chargingSource`     | String  | `"AC"`        |
| `connectivity.transportType` | String  | `"WIFI"`      |
| `status.online`              | Boolean | `true`        |
| `status.monitoringRunning`   | Boolean | `false`       |
| `location.latitude`          | Double  | `-6.2`        |
| `location.longitude`         | Double  | `106.8`       |
| `location.accuracy`          | Float   | `12.5`        |

## Operators

| Operator         | Symbol           | Notes                                                              |
|------------------|------------------|--------------------------------------------------------------------|
| `LT`             | `<`              | Numeric comparison                                                 |
| `LTE`            | `<=`             | Numeric comparison                                                 |
| `GT`             | `>`              | Numeric comparison                                                 |
| `GTE`            | `>=`             | Numeric comparison                                                 |
| `EQ`             | `=`              | Exact match (string or numeric)                                    |
| `NEQ`            | `!=`             | Not equal                                                          |
| `CONTAINS`       | `contains`       | String contains substring                                          |
| `IS_TRUE`        | `is true`        | Unary — field is boolean true                                      |
| `IS_FALSE`       | `is false`       | Unary — field is boolean false                                     |
| `INSIDE_RADIUS`  | `inside radius`  | Geofence: condition value = `lat,lng`, radiusMeters set separately |
| `OUTSIDE_RADIUS` | `outside radius` | Geofence: condition value = `lat,lng`, radiusMeters set separately |

## Action Parameters

### Device-scope actions

| Action                   | Required params | Description                                     |
|--------------------------|-----------------|-------------------------------------------------|
| `START_LOCATION_SHARING` | —               | Starts the location foreground service          |
| `STOP_LOCATION_SHARING`  | —               | Stops the location foreground service           |
| `SET_CONFIG`             | `key`, `value`  | Sets a single config field                      |
| `START_MONITORING`       | —               | Starts MonitoringService                        |
| `REPORT_EVENT`           | `message`       | Reports a custom event to Firebase and Telegram |

### Control-scope actions

| Action              | Required params | Description                               |
|---------------------|-----------------|-------------------------------------------|
| `SEND_COMMAND`      | `command`       | Sends a command via FCM                   |
| `SEND_NOTIFICATION` | `title`, `body` | Sends a notification via FCM              |
| `SET_CONFIG`        | `key`, `value`  | Updates a device config field in Firebase |
| `LOCAL_ALERT`       | `message`       | Shows a local notification in AppControl  |
| `REPORT_EVENT`      | `message`       | Logs an event                             |

## Examples

### Geofence Exit Alert

```json
{
  "name": "Left home area",
  "scope": "CONTROL",
  "match": "ALL",
  "conditions": [
    {
      "field": "location.latitude",
      "operator": "OUTSIDE_RADIUS",
      "value": "-6.200,106.816",
      "radiusMeters": 500
    }
  ],
  "actions": [
    {
      "type": "LOCAL_ALERT",
      "params": {
        "message": "Device left home area"
      }
    }
  ],
  "cooldownMs": 600000,
  "edgeTriggered": true
}
```

### Low Battery Auto-Stop Location

```json
{
  "name": "Stop location on low battery",
  "scope": "DEVICE",
  "match": "ALL",
  "conditions": [
    {
      "field": "battery.level",
      "operator": "LTE",
      "value": "10"
    }
  ],
  "actions": [
    {
      "type": "STOP_LOCATION_SHARING",
      "params": {}
    }
  ],
  "cooldownMs": 300000,
  "edgeTriggered": true
}
```

### Start Monitoring on Wi-Fi

```json
{
  "name": "Start monitoring when on WiFi",
  "scope": "DEVICE",
  "match": "ALL",
  "conditions": [
    {
      "field": "connectivity.transportType",
      "operator": "EQ",
      "value": "WIFI"
    }
  ],
  "actions": [
    {
      "type": "START_MONITORING",
      "params": {}
    }
  ],
  "cooldownMs": 300000,
  "edgeTriggered": true
}
```

## Cooldown and Edge-Triggering

**Cooldown** prevents a rule from firing too frequently. After a rule fires, it will not fire again
for at least `cooldownMs` milliseconds, even if the conditions remain true.

**Edge-triggered** (when `true`) means the rule will only fire again after its conditions became
false in between. This prevents repeated firing while a condition remains steadily true. When
`false`, the rule re-fires after every cooldown expiry, as long as conditions are still met.
