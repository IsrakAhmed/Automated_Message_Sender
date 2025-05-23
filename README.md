
# Automated SMS Sender

This project demonstrates an Android app that can send SMS messages programmatically, triggered remotely from a PC Python script. It supports selecting SIM for dual SIM devices and runs fully in the background without opening the SMS app UI.

---

## How It Works

### Overview

- The **Android app** receives commands via an Intent containing the phone number, message, and SIM selection.
- The app sends the SMS programmatically using Android's `SmsManager` API.
- The **PC Python script** triggers the Android app using ADB shell commands, passing the SMS details as Intent extras.
- The app sends the SMS and closes itself automatically.

---

### Android App Components

- **MainActivity.java**
  - Checks for SMS permissions and requests them if necessary.
  - Reads phone number, message, and optional SIM info from Intent extras.
  - Uses `SubscriptionManager` to get active SIM info and send SMS via the selected SIM.
  - Sends SMS using `SmsManager` tied to the chosen subscription ID.
  - Displays Toast notifications for success or failure.
  - Closes itself after sending the message.

- **AndroidManifest.xml**
  - Declares required permissions: `SEND_SMS` and `READ_PHONE_STATE`.
  - Defines `MainActivity` with exported attribute for Android 12+ compatibility.

---

### Python PC Client

- Uses `subprocess` to run `adb` shell commands.
- Sends an Intent to the Android device with extras: `phone_number`, `message`, and optional `sim_index`.
- Example command:

  ```bash
  adb shell am start -n com.sentinel.smssender/.MainActivity \
    --es phone_number "01700000000" \
    --es message "Hello from PC!" \
    --ei sim_index 1
  ```

- This triggers the app to send the SMS using the specified SIM.

---

## Data Flow Summary

1. **PC Python script** sends an ADB command to start the Android app with SMS details.
2. **Android app** reads the Intent extras on launch.
3. App verifies permissions, selects the SIM based on `sim_index`.
4. Sends the SMS programmatically via `SmsManager`.
5. Shows a toast confirmation.
6. Closes itself.

---

## Key Features

- **Background SMS sending** without UI.
- **Dual SIM support**: select which SIM to send SMS from.
- **Runtime permission handling**.
- **Easy integration** with PC via ADB commands.

---

## Requirements

- Android device with SMS capability and ADB enabled.
- PC with ADB installed.
- Android app installed on device.
- Permissions granted by user on app launch.

---

## Next Steps / Enhancements

- Add UI for manual input and SIM selection.
- Handle SMS sending status callbacks.
- Secure communication between PC and device.
- Support multipart SMS messages.

---

Feel free to open issues or contribute!
