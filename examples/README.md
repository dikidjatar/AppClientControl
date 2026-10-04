# Examples

This folder contains ready-to-use JSON examples for the main AppClientControl workflows.

## Payloads

FCM notification payloads sent from AppControl to AppClient. Each JSON file corresponds to a
`FcmNotificationPayload` object.

In AppControl, payloads are constructed via the Notification Composer UI. The files here are
provided for reference, documentation, and testing purposes.

| File                                | Description                                 |
|-------------------------------------|---------------------------------------------|
| `payloads/simple_notification.json` | Minimal title + body notification           |
| `payloads/big_text.json`            | Expanded text body using `big_text` style   |
| `payloads/messaging_style.json`     | Conversation thread with multiple messages  |
| `payloads/progress_bar.json`        | Ongoing progress bar notification           |
| `payloads/with_actions.json`        | Notification with three action buttons      |
| `payloads/upload_file.json`         | Prompt the device to pick and upload a file |
| `payloads/download_file.json`       | Push a file download to the device          |
| `payloads/open_webview.json`        | Open a remote URL in a WebView overlay      |
| `payloads/automation_rule.json`     | Sample automation rules (Firebase schema)   |

## How to Send a Payload Manually via FCM HTTP v1

If you want to send a payload outside AppControl (for testing or scripting), use the FCM HTTP v1
endpoint with your service account token:

```bash
# 1. Get an access token from the service account
ACCESS_TOKEN=$(python3 - <<'PYTHON'
from google.oauth2 import service_account
import google.auth.transport.requests

creds = service_account.Credentials.from_service_account_file(
    "service_account.json",
    scopes=["https://www.googleapis.com/auth/firebase.messaging"]
)
req = google.auth.transport.requests.Request()
creds.refresh(req)
print(creds.token)
PYTHON
)

# 2. Send the message
curl -X POST \
  "https://fcm.googleapis.com/v1/projects/YOUR_PROJECT_ID/messages:send" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "token": "DEVICE_FCM_TOKEN",
      "data": {
        "notificationId": "test_001",
        "title": "Test",
        "body": "Hello from the command line",
        "style": "default",
        "priority": "HIGH"
      }
    }
  }'
```

The `data` object keys correspond to the `FcmNotificationPayload` field names. Replace
`YOUR_PROJECT_ID` with your Firebase project ID and `DEVICE_FCM_TOKEN` with the target device's FCM
token (visible in AppControl's Capabilities tab).
