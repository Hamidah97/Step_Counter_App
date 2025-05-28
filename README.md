# Step Counter App 📱🚶‍♀️

This project is developed as part of a **Technical Skill Assessment** to demonstrate beginner-level mobile development skills. It is a simple step counting system that collects data from a mobile device, sends it to a cloud database, and visualizes it via a web interface.

---

## 🌟 Project Overview

The system has 3 main components:

1. **Android Mobile App**: Collects step count data using the device's sensor.
2. **Cloud Database**: Firebase Realtime Database stores step data sent from the mobile app.
3. **Web Page**: Retrieves data from Firebase and displays it in a chart.

---

## 📱 Mobile App (Android)

### Features:
- Uses built-in **Step Counter Sensor**
- Stores data temporarily on the device
- Sends data periodically to Firebase

### Tools:
- Android Studio
- Java/Kotlin
- Firebase SDK

### Setup:
1. Open the project in Android Studio
2. Add your own `google-services.json` to the `app/` folder (if using Firebase)
3. Make sure Firebase is enabled and the Realtime Database is set up
4. Run the app on a device (physical device preferred for step sensor)

---

## ☁️ Cloud Backend

### Service:
- **Firebase Realtime Database**

### Structure:
- Each data entry includes:
  - `timestamp`: Time of recording
  - `stepCount`: Total steps counted
  - `userId`: (optional field for future use)

---

## 🌐 Web Visualization

- A simple web page built with HTML/JavaScript
- Uses **Chart.js** to render a line chart of steps over time
- Retrieves data from Firebase via REST API

---

## 🛠 How to Run

### Mobile App:
- Run the project in Android Studio on a real Android device with step counter sensor
- Ensure internet access to send data to Firebase

### Web App:
- Open the HTML file in any browser
- It will auto-fetch data and display the chart

---

## 📂 Folder Structure

