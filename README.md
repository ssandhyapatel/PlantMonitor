# PlantMonitor — Plant-Wearable IoT Dashboard (Android + ESP32)

[![Android](https://img.shields.io/badge/Android-12%2B-3DDC84)]() [![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF)]() [![ESP32](https://img.shields.io/badge/MCU-ESP32-orange)]() [![BLE](https://img.shields.io/badge/Comm-BLE-1f6feb)]() [![MPAndroidChart](https://img.shields.io/badge/Charts-MPAndroidChart-blue)]() [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)]()

**PlantMonitor** is a lightweight Android application that connects to a plant-wearable ESP32 sensor node over **Bluetooth Low Energy (BLE)** to visualize **VOC**, **leaf temperature**, and **humidity** in real time. The app shows a clean dashboard, live charts, and threshold-based alerts—useful for research, demos, and early plant stress detection in smart agriculture.

---

## ✨ Features

- **Real-time telemetry:** VOC, Temperature (°C), Humidity (%)  
- **Live charts:** Smooth line plots using MPAndroidChart  
- **Alerts:** Color-coded status + notifications on threshold breach  
- **BLE device management:** Scan, connect, reconnect, connection status  
- **History (basic):** Alert log; (optional) CSV/SQLite hooks for local storage  
- **Configurable thresholds:** Manual limits for VOC/Temp/Humidity  
- **Field-friendly UI:** Clean, responsive, offline-first design

---

## 🏗️ System Architecture

```
Sensors (DHT22, MP503) --> ESP32 (BLE Server)
           |                     |
           +----> Packet: voc,temp,humidity (CSV)
                                 |
                          Android App (BLE Client)
                         Dashboard • Alerts • Charts
```

- **Hardware:** ESP32 DevKit v1, DHT22, MP503 VOC (AOUT → GPIO34), breadboard/jumpers  
- **Firmware:** Arduino/ESP-IDF; BLE GATT server broadcasting CSV line `voc_raw,temp_celsius,humidity_percent` at ~1–3 s intervals  
- **Mobile:** Android (Kotlin + XML), BLE APIs, MPAndroidChart

---

## 📂 Repository Structure (suggested)

```
PlantMonitor/
├─ android-app/
│  ├─ app/src/main/java/com/example/plantmonitor/
│  │  ├─ data/               # data models, storage helpers
│  │  ├─ ble/                # BLEManager, GATT callbacks
│  │  ├─ ui/                 # activities/fragments/adapters
│  │  └─ utils/              # formatters, permissions, etc.
│  ├─ app/src/main/res/      # layouts, drawables, values
│  └─ build.gradle
├─ firmware/
│  └─ esp32_ble_sensor.ino   # ESP32 firmware (example)
├─ docs/
│  ├─ architecture.png
│  ├─ screenshots/
│  │  ├─ dashboard.png
│  │  ├─ trends.png
│  │  └─ alerts.png
│  └─ demo.md
└─ README.md
```

> Adjust paths/names to match your actual repo layout.

---

## 🔌 ESP32 Firmware (Quick Start)

1. **Wiring**
   - DHT22 → `VCC (3.3V)`, `GND`, `DATA → GPIO15` with **10k pull-up** between `VCC` and `DATA`
   - MP503 (analog VOC) → `AOUT → GPIO34` (use 3.3V)
2. **Libraries**
   - `DHT` (Adafruit or equivalent), `BLEDevice` (ESP32 BLE)
3. **BLE Settings**
   - Device name: `PlantSensor_01` (example)  
   - **Notify** a GATT characteristic with CSV payload:
     ```
     <voc_raw>,<temp_celsius>,<humidity_percent>\n
     ```
     e.g. `57,31.4,45.2`
4. **Sampling**
   - Start at 1–3 s intervals; tune for battery/latency

> Keep the output stable and include a newline `\n`—the app’s parser splits on comma and trims lines.

---

## 📱 Android App (Build & Run)

### Requirements
- **Android Studio Flamingo/Koala or newer**
- **Android SDK 24+** (target 34+), **Gradle 8+**
- **Kotlin 1.9+**
- Test device with **BLE** (Android 8.0/Oreo or newer recommended)

### Permissions
Add (already in template if provided):
```xml
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```
> On Android 12+, `BLUETOOTH_SCAN` & `BLUETOOTH_CONNECT` are runtime-granted. Some devices require **Location** for BLE scans.

### Steps
1. Open `android-app/` in Android Studio  
2. Sync Gradle; resolve dependencies (MPAndroidChart, AndroidX, etc.)  
3. Build & install on a physical device (recommended for BLE)  
4. Turn on Bluetooth; power the ESP32 node  
5. In the app: **Scan → Connect** to `PlantSensor_01` (or your name)  
6. Watch **Dashboard** values and **Trends** live charts update

---

## ⚙️ Configuration

**Thresholds (defaults; edit in Settings or constants):**
- `VOC_WARN = 50` (raw units, tune based on calibration)
- `TEMP_WARN = 35°C`
- `HUMIDITY_WARN = 40%`

**Packet format (required):**
```
voc_raw,temp_celsius,humidity_percent
```
- Example: `72,36.1,38.0`
- If adding sensors (CO₂, soil moisture, pH), **append** values and update the parser in the app.

---

## 🖼️ Screenshots

> Put your real images under `docs/screenshots/` and reference here.

| Dashboard | Trends | Alerts |
| --- | --- | --- |
| ![Dashboard](docs/screenshots/dashboard.png) | ![Trends](docs/screenshots/trends.png) | ![Alerts](docs/screenshots/alerts.png) |

---

## 🧪 Demo

- **Short demo video:** *(Demonstration/Plant-Monitor Video.mp4)*  
- **Pitch/Report PDFs:** *(optional links to `docs/` or releases)*

---

## 🛠️ Tech Stack

- **Mobile:** Android (Kotlin, XML), Android BLE, MPAndroidChart  
- **MCU:** ESP32 DevKit v1 (Arduino/ESP-IDF)  
- **Data:** CSV payload over BLE notifications  
- **Design:** Clean UI, offline-friendly architecture

---

## 🚧 Roadmap

- [ ] Stable reception path + reconnection strategy  
- [ ] Local storage (SQLite/Room) + CSV export  
- [ ] Cloud sync + remote dashboard  
- [ ] ML-based stress classification (VOC/Temp/Humidity trends)  
- [ ] Multi-sensor profiles (CO₂, soil moisture, pH)  
- [ ] Test harness + instrumentation tests

---

## 🧰 Troubleshooting

- **No devices found on scan**
  - Ensure Bluetooth + Location are enabled; grant runtime permissions
  - ESP32 is powered and advertising; reduce distance (<3–5 m initially)
- **Connects but no data**
  - Verify the **notify** characteristic and CSV format with newline
  - Check characteristic UUIDs match the app (if you use custom UUIDs)
- **Weird values**
  - DHT22 wiring & pull-up resistor; sensor warm-up time
  - MP503 analog output noise—average readings or add a simple low-pass
- **Crashes on older Android**
  - Use physical device with Android 8.0+; check permissions for Android 12+

---

## 🤝 Contributing

1. Fork the repo and create a feature branch  
2. Follow Kotlin code style; keep UI XML clean and accessible  
3. Add screenshots/notes to PR where relevant  
4. Open a PR with a clear description and testing steps

---

## 📜 License

This project is licensed under the **MIT License**. See `LICENSE` for details.

---

## 🙌 Acknowledgements

- Espressif (ESP32), MPAndroidChart  
- Mentorship: **Dr. Debanjan Acharyya**  
- Institution: **IGDTUW / NIT Agartala**  
- Contributors & testers

---

## 📫 Contact

- **Maintainer:** Sandhya Patel  
- **Email:** 001sandhyapatel@gmail.com  

