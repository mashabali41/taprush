# ⚡ Tap Rush

A simple Android game built in **Java**. Tap the button as many times as you can in 10 seconds. Sign in with email or Google, and your high score is saved to your account with Firebase so it follows you across devices.

## Features

- Splash screen that routes you to the game or login automatically
- Sign up and log in with **email/password**
- **Continue with Google** on both the login and sign-up screens
- Persistent sessions (stay signed in between launches)
- 10-second tap game with a live countdown and score
- High score saved per user in **Cloud Firestore**
- Sign out button

## Screenshots

<!-- Add your screenshots here, e.g. put images in a /screenshots folder -->
| Splash | Login | Sign up | Game |
|--------|-------|---------|------|
| _add_  | _add_ | _add_   | _add_ |

## Tech stack

- Java, AndroidX, Min SDK 24
- Gradle (Kotlin DSL)
- Firebase Authentication (Email/Password + Google Sign-In)
- Cloud Firestore

## Project structure

```
app/src/main/java/com/example/taprush/
├── SplashActivity.java      # Launcher, routes to Login or Game
├── BaseAuthActivity.java    # Shared Google sign-in logic
├── LoginActivity.java       # Email + Google login
├── SignUpActivity.java      # Email sign up + Google
└── GameActivity.java        # The game + high score saving

app/src/main/res/layout/
├── activity_splash.xml
├── activity_login.xml
├── activity_signup.xml
└── activity_game.xml
```

## Getting started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- A Google account for the Firebase console
- An emulator image with **Google Play** (the Play Store icon in the AVD list) or a real Android phone

### 1. Clone the repo

```bash
git clone https://github.com/<your-username>/taprush.git
cd taprush
```

Open the folder in Android Studio and let Gradle sync.

### 2. Create a Firebase project

1. Go to the [Firebase console](https://console.firebase.google.com) and create a project.
2. Add an **Android app** using your exact package name (default: `com.example.taprush`).
3. Add your **debug SHA-1** fingerprint under **Project settings → Your apps → SHA certificate fingerprints**.

   Get it in Android Studio from the Gradle panel: **app → Tasks → android → signingReport** (look under *Variant: debug*). Or from a terminal:

   ```bash
   ./gradlew signingReport
   ```

   If the terminal says `JAVA_HOME is not set`, use the Gradle panel instead, or point `JAVA_HOME` at Android Studio's bundled JDK (the `jbr` folder in its install directory).

### 3. Enable sign-in methods

In **Build → Authentication → Sign-in method**, enable:

- **Email/Password**
- **Google** (set a public-facing name and support email)

### 4. Create Firestore

1. Go to **Build → Firestore Database → Create database**.
2. Choose a location and **Start in production mode**.
3. Open the **Rules** tab, paste the following, and click **Publish**:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /users/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

Each user can only read and write their own document at `users/{uid}`. The app creates it the first time a player sets a high score.

### 5. Add `google-services.json`

Download `google-services.json` from **Project settings → Your apps** (after enabling Google sign-in and adding the SHA-1) and place it in the `app/` folder. The file must be named exactly `google-services.json`.

> ⚠️ This file is specific to your Firebase project. Many people add it to `.gitignore` when sharing a repo publicly. If you do, contributors need to create their own Firebase project and add their own copy.

### 6. Run

Sync Gradle, pick an emulator with Google Play (or a real device), and press **Run ▶**.

## Data model

Collection `users`, one document per user (document ID = Firebase UID):

| Field       | Type   | Description                         |
|-------------|--------|-------------------------------------|
| `highScore` | number | Best score the player has achieved  |
| `name`      | string | Display name (or email as fallback) |

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Google sign-in error code `10` or `12500` | SHA-1 is missing or wrong in Firebase. Add it, then re-download `google-services.json`. |
| `default_web_client_id` not found | Google sign-in wasn't enabled when you downloaded the JSON. Enable it, re-download, and sync. |
| `Plugin 'com.android.application' was already requested` | A plugin is listed twice in a `plugins {}` block. Keep each plugin once per file. |
| Gradle sync fails while downloading | The Gradle distribution is large. Let it finish, then click **Try Again**. |
| `Permission denied` from Firestore | Check the rules are published and you are signed in. |
| Sign-in does nothing on emulator | Use an emulator image that includes Google Play. |
| File called `google-services (1).json` | Rename it to `google-services.json` and delete older copies. |

## Roadmap

- [ ] Forgot password / password reset
- [ ] Email verification
- [ ] Global leaderboard
- [ ] Profile photo from Google account
- [ ] Migrate Google sign-in to Credential Manager (the `GoogleSignIn` API is deprecated)

## Contributing

Pull requests are welcome. For larger changes, please open an issue first to discuss what you'd like to change.

## License

This project is licensed under the [MIT License](LICENSE).
