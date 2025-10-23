# 📱 TrackUTeM Mobile App

![License: MIT](https://img.shields.io/github/license/ST-0301/ProjectFYP-TrackUTeM?style=for-the-badge)
![GitHub issues](https://img.shields.io/github/issues/ST-0301/ProjectFYP-TrackUTeM?style=for-the-badge&color=brightgreen)
![GitHub stars](https://img.shields.io/github/stars/ST-0301/ProjectFYP-TrackUTeM?style=for-the-badge)

**TrackUTeM Mobile App** is part of the **UTeM Bus Tracking and Time Estimation System**. This Android app provides real-time bus tracking for **students** and **drivers**, while the companion web system is used by **admin officers** for route and schedule management.

Built with **Android Studio (Java)** and **Firebase**, it aims to improve the campus commuting experience by providing accurate bus arrival times and seamless communication.

---

## 📸 Screenshots
*(Add screenshots here later, for example:)*
* Student Home Screen
* Driver Interface
* Notifications Page

---

## 📦 APK Download

You can install the app directly on an Android device:

[![Download APK](https://img.shields.io/badge/Download%20APK-3DDC84?style=for-the-badge&logo=android&logoColor=white)](./app/release/app-release.apk)

---

## ✨ Features

### 👨‍🎓 For Students
* 🚌 View real-time bus locations
* ⏱️ Check estimated arrival times
* 🔔 Receive bus notifications (delays, breakdowns, etc.)

### 👨‍✈️ For Drivers
* 📅 View assigned route and schedule
* ✅ Update bus status (departed, arrived)
* 🔔 Receive notifications from admin (new assignmnts, cancelled assignments)

---

## 🛠️ Tech Stack

This project is built using modern mobile and cloud technologies:

![Android Studio](https://img.shields.io/badge/Android_Studio-3DDC84?style=for-the-badge&logo=androidstudio&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

* **Framework:** Android (Java)
* **Backend & Database:** Firebase Firestore
* **Authentication:** Firebase Auth
* **Notifications:** Firebase Cloud Messaging (FCM)
* **Location Tracking:** Google Maps & Location Services

---

## 🚀 Getting Started

Follow these steps to set up and run the project locally.

### Prerequisites
Make sure you have:
* [Android Studio](https://developer.android.com/studio) installed
* A configured [Firebase](https://firebase.google.com/) project with:
    * Firestore
    * Authentication
    * Cloud Messaging

### 🧩 Setup Instructions

1.  **Clone the repository**
    ```bash
    git clone https://github.com/ST-0301/ProjectFYP-TrackUTeM.git
    cd ProjectFYP-TrackUTeM
    ```

2.  **Open in Android Studio**
    * In Android Studio, select "Open an existing project".
    * Navigate to and select the cloned `ProjectFYP-TrackUTeM` folder.

3.  **Add Firebase Configuration**
    * Download your `google-services.json` file from your project in the Firebase Console.
    * Place this file inside the `/app` directory of the project.

4.  **Build the App**
    * Go to **Build > Make Project** or run the Gradle task:
    ```bash
    ./gradlew assembleRelease
    ```
    * The installable APK will be generated at `app/release/app-release.apk`.

---

## 🗃️ Firebase Database Structure

The project uses a shared Firebase backend with the web admin panel. The data is organized into the following top-level collections:

| Collection | Purpose |
| :--- | :--- |
| `/admins` | Stores administrator user accounts and permissions. |
| `/drivers` | Stores driver information (name, contact, etc.). |
| `/buses` | Stores details for each bus (plate number, capacity). |
| `/routes` | Defines the different bus routes (e.g., "Hop On Induk"). |
| `/routePoints` | Stores the specific geographic coordinates (stops) for each route. |
| `/schedules` | Manages the bus schedules, linking routes, buses, and times. |
| `/busDriverPairings` | Tracks the real-time assignment of a specific driver to a specific bus. |
| `/users` | Stores student user accounts (if login is required). |

**Note:** You do not need to manually create these collections. Firestore will automatically create them when the first document is added by the application.

---

## 🔗 Related Projects

This project is part of a larger system.

| Platform | Repository | Description |
| :--- | :--- | :--- |
| 🌐 **Web** | [ProjectFYP-TrackUTeMWeb](https://github.com/st-0301/ProjectFYP-TrackUTeMWeb) | Admin panel for managing buses, drivers, routes, and schedules. |
| 📱 **Mobile** | [ProjectFYP-TrackUTeM](https://github.com/ST-0301/ProjectFYP-TrackUTeM) | Android app for students and drivers. |

---

## 👩‍💻 Author

Ng Sue Ting
Final Year Project — Universiti Teknikal Malaysia Melaka (UTeM)

🔗 GitHub: [@ST-0301](https://github.com/st-0301)

🌐 Web System: [TrackUTeM Web Demo](https://st-0301.github.io/ProjectFYP-TrackUTeMWeb/)

---

## 📄 License

This project is released under the MIT License.
You are free to use and modify this code for educational or non-commercial purposes with proper attribution.
