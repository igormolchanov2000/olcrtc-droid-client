# Android run checklist

## Emulator
- [ ] `adb devices` shows `emulator-5554` (or another emulator) in state `device`
- [ ] `cd android && ./gradlew assembleDebug` completes successfully
- [ ] `cd android && ANDROID_SERIAL=emulator-5554 ./scripts/install-and-launch.sh` installs and launches the app
- [ ] the main screen shows fields for **Telemost room ID** and **Secret key**
- [ ] `cd android && ANDROID_SERIAL=emulator-5554 ./scripts/logs.sh` shows `olcrtc` log lines when the service runs
- [ ] tapping **Connect** with invalid input shows validation errors without starting the service
- [ ] tapping **Connect** with valid-looking input triggers the Android VPN permission flow
- [ ] after disconnect, the foreground notification disappears and the UI returns to `IDLE` or `ERROR`

## Wi‑Fi debugging device
- [ ] `adb devices` shows the phone as `device`
- [ ] `cd android && ANDROID_SERIAL='<wifi-serial>' ./scripts/install-and-launch.sh` installs and launches the app
- [ ] `cd android && ANDROID_SERIAL='<wifi-serial>' ./scripts/logs.sh` attaches to logcat
- [ ] the VPN permission prompt is shown on-device
- [ ] the foreground notification remains visible while connected or connecting
- [ ] an olcrtc connect/disconnect cycle works with real room/key credentials

## Native rebuild workflow
- [ ] `cd android && ./scripts/build-mobile-bindings.sh` regenerates `app/libs/olcrtc-mobile.aar`
- [ ] `cd android && ./scripts/build-tun2socks.sh` regenerates JNI libraries under `app/src/main/jniLibs/`
- [ ] `./gradlew assembleDebug` re-runs both native preparation steps automatically

## Manual adb commands
- [ ] `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- [ ] `adb shell am start -n org.openlibrecommunity.olcrtc/.MainActivity`
- [ ] `adb logcat -s olcrtc:D AndroidRuntime:E ActivityManager:I *:S`
