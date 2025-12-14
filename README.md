<div align="center">

# CyberGuard
<img height="200" alt="logo" src="https://github.com/user-attachments/assets/6d012f41-4c54-462e-92fb-9643afbc819d" />

**A cybersecurity companion Android app providing breach checks, encrypted notes, phishing training, password tools, and network risk insights.**

![Java](https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?logo=firebase&logoColor=black)

</div>

## Table of Contents

- [Project Description](#project-description)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [1. Network Connectivity Check](#1-network-connectivity-check)
- [2. Landing Page](#2-landing-page)
- [3. Setting up Authentication with Firebase](#3-setting-up-authentication-with-firebase)
- [4. Navigation Drawer](#4-navigation-drawer)
- [5. Dashboard and User Profile](#5-dashboard-and-user-profile)
  - [5.1. Dashboard and Features](#51-dashboard-and-features)
    - [Breach Check](#breach-check)
    - [Secure Notes](#secure-notes)
    - [Cyber News](#cyber-news)
    - [Phishing Training](#phishing-training)
    - [Network Security](#network-security)
    - [Cyber Quiz](#cyber-quiz)
    - [Password Generator](#password-generator)
  - [5.2. User Profile and Editing](#52-user-profile-and-editing)
- [6. Other Navigation Drawer Features](#6-other-navigation-drawer-features)
- [License](#license)
- [Contributing](#contributing)

## Project Description

CyberGuard is a comprehensive Android application designed to enhance users' digital security. It offers a suite of tools to protect against common threats, such as data breaches and phishing attacks. This document provides a detailed description of the project's development process, the code used, and the implemented features.

This project uses Firebase for several key features, including:

*   **Firebase Authentication**: For user account management, including register and login via email and password.
*   **Firebase Firestore**: As a NoSQL database to store user information, such as full name, email address, and phone number.

## Tech Stack

*   **Java**: The primary programming language for application development.
*   **Android SDK**: For creating the native user interface and managing application components.
*   **Firebase**:
    *   **Authentication**: To manage user register and login.
    *   **Firestore**: To store user data.
*   **Third-Party APIs**:
    *   **Pwned Passwords**: To check if a password has been compromised.
    *   **XposedOrNot**: To check for email data breaches.

## Features

- 🔐 **Secure Authentication**: Register and login with Firebase.
- 📊 **Dashboard**: Quick access to all security features.
- 📧 **Breach Check**: Check if your email or password has been compromised in a data breach.
- 🎣 **Phishing Training**: Learn to recognize and avoid phishing attempts.
- 👤 **Profile Management**: Update your personal information.
- 🌐 **Connection Check**: Ensures that online features are only available with an internet connection.
- 🧭 **Intuitive Navigation**: A navigation drawer for easy access to all sections.

---

## 1. Network Connectivity Check

To ensure a smooth user experience, the application checks for an internet connection before performing network operations. The `NetworkUtils` class provides a static method to determine if the device is connected to the internet.

<p align="center">
  <img src="https://github.com/user-attachments/assets/881a86e1-6ef8-4cb9-9deb-c2f42a6452fa"
       alt="image"
       height="600" />
</p>

The code below shows how connectivity is checked.

```java
public static boolean hasInternetConnection(Context context) {
    if (context == null) return false;

    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm == null) return false;

    Network network = cm.getActiveNetwork();
    if (network == null) return false;

    NetworkCapabilities caps = cm.getNetworkCapabilities(network);
    if (caps == null) return false;

    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
}
```

## 2. Landing Page

The landing page is the first screen a user sees. It checks if a user is already signed in. If so, it redirects them to the dashboard. Otherwise, it presents options to sign in or sign up.

<p align="center">
  <img src="https://github.com/user-attachments/assets/b4d4b162-ffc3-48ef-ab7e-7dad1925a9d3"
       alt="image"
       height="600" />
</p>

```java
FirebaseUser currentUser = mAuth.getCurrentUser();
if (currentUser != null) {
    startActivity(new Intent(LandingActivity.this, DrawerActivity.class));
    finish();
}
```

## 3. Setting up Authentication with Firebase

### 3.1. Sign Up (Register)

The register page allows new users to create an account. It collects the user's full name, email address, phone number, and a password.

<p align="center">
  <img src="https://github.com/user-attachments/assets/36450fe7-5bb6-4dbe-836f-ff0be71aff9d"
       alt="image"
       height="600" />
</p>

The code below shows how a new user is created with Firebase Authentication and how their information is saved to Firestore.

```java
mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
    if (task.isSuccessful()) {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", fullName);
        user.put("email", email);
        user.put("phone", fullPhone);

        db.collection("users").document(uid).set(user).addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
});
```

### 3.2. Login

The login page allows users to sign in with their email and password.

<p align="center">
  <img src="https://github.com/user-attachments/assets/81b1f7b4-ead5-4a57-904d-acf0b82c1516"
       alt="image"
       height="600" />
</p>

The code below handles user sign-in with Firebase Authentication.

```java
mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
    if (task.isSuccessful()) {
        startActivity(new Intent(LoginActivity.this, DrawerActivity.class));
        finish();
    }
});
```

## 4. Navigation Drawer

The navigation drawer is the main menu. It displays user information and provides access to different sections and the logout option.

<p align="center">
  <img src="https://github.com/user-attachments/assets/c02e9de1-5f07-4962-8fef-8aabf5c5aa8c"
       alt="image"
       height="600" />
</p>

The following code loads user information into the drawer's header.

```java
protected void loadUserInfoInDrawer() {
    FirebaseUser currentUser = mAuth.getCurrentUser();
    View headerView = navigationView.getHeaderView(0);
    TextView tvFullName = headerView.findViewById(R.id.drawer_tvFullName);
    TextView tvEmail = headerView.findViewById(R.id.drawer_tvEmail);
    tvEmail.setText(currentUser.getEmail());

    userListener = db.collection("users")
            .document(currentUser.getUid())
            .addSnapshotListener((document, e) -> {
                if (document != null && document.exists()) {
                    tvFullName.setText(document.getString("fullName"));
                }
            });
}
```

## 5. Dashboard and User Profile

### 5.1. Dashboard and Features

The dashboard is the main screen after login. It displays a grid of features, each with an icon and a description, serving as a launchpad for the application's various tools.

<p align="center">
  <img src="https://github.com/user-attachments/assets/22804895-77a5-4389-898d-a7e3a052743b"
       alt="image"
       height="600" />
</p>

#### Breach Check
🔐 Allows users to check if an email has been exposed in a data breach using the *XposedOrNot* API, or if a password has been compromised using the *Pwned Passwords* API.
<p align="center">
  <img src="https://github.com/user-attachments/assets/1432f5ec-1c0a-492b-8c9b-e0673ee23ca4"
       alt="image"
       height="600" />
</p>

```java
private long checkPwnedPasswordCount(String password) throws Exception {
    String sha1 = sha1Hex(password).toUpperCase(Locale.US);
    String prefix = sha1.substring(0, 5);
    String suffix = sha1.substring(5);

    URL url = new URL("https://api.pwnedpasswords.com/range/" + prefix);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");

    int code = conn.getResponseCode();
    if (code != 200) throw new Exception("Password API error: HTTP " + code);

    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String line;
    while ((line = reader.readLine()) != null) {
        int colonIndex = line.indexOf(':');
        if (colonIndex > 0) {
            String apiSuffix = line.substring(0, colonIndex).trim();
            if (apiSuffix.equalsIgnoreCase(suffix)) {
                String countStr = line.substring(colonIndex + 1).trim();
                return Long.parseLong(countStr);
            }
        }
    }
    return 0;
}
```

#### Secure Notes
📝 Allows users to store encrypted notes. Access is protected by a master password set by the user on first use. Once unlocked, the user can create, view, and manage their secure notes.
<p align="center">
  <img src="https://github.com/user-attachments/assets/ddce5bf5-01dc-4870-b297-a24d1be9cce5" alt="image" height="600" style="margin: 0 10px;" />
  <img src="https://github.com/user-attachments/assets/3ce9a098-cad8-490e-956e-c1f1dcfa3041" alt="image" height="600" style="margin: 0 10px;" />
  <img src="https://github.com/user-attachments/assets/0b76ca59-7394-4591-a4b3-0bc0b3160ac5" alt="image" height="600" style="margin: 0 10px;" />
</p>

```java
private void unlockOrSetup() {
    String master = etMaster.getText().toString();
    // ... (validation)

    repo.getOrCreateSalt(new SecureNotesRepository.Callback<String>() {
        @Override
        public void onSuccess(String saltB64) {
            new Thread(() -> {
                try {
                    SecretKey key = NotesCrypto.deriveKey(master, saltB64);

                    if (isFirstTimeSetup) {
                        // Create and store an encrypted verifier
                        NotesCrypto.EncResult enc = NotesCrypto.encrypt(VAULT_CHECK_PLAINTEXT, key);
                        repo.setVaultCheck(enc.cipherTextB64, enc.ivB64, /* ... */);
                    } else {
                        // Verify the password by decrypting the stored verifier
                        repo.getVaultCheck(new SecureNotesRepository.Callback<Map<String, String>>() {
                            @Override
                            public void onSuccess(Map<String, String> v) {
                                // ... (decrypt and compare)
                            }
                        });
                    }
                } catch (Exception e) {
                    // ... (handle errors)
                }
            }).start();
        }
    });
}
```

#### Cyber News
📰 Launches an Android `Intent` to open the user's web browser and display *The Hacker News* website, providing the latest cybersecurity news.

<p align="center">
  <img src="https://github.com/user-attachments/assets/18f720c0-f0e0-4a15-8927-15057399decd"
       alt="image"
       height="600" />
</p>

```java
case "cyber_news":
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse("https://thehackernews.com"));
    startActivity(intent);
    break;
```

#### Phishing Training
🎣 Educates users on the tell-tale signs of a phishing attack. The content is presented in expandable sections for easy reading.

<p align="center">
  <img src="https://github.com/user-attachments/assets/111f59e8-9fd4-4769-8594-fe068885b61d"
       alt="image"
       height="600" />
</p>

```java
private void toggle(LinearLayout body, TextView header, String title) {
    boolean open = body.getVisibility() == View.VISIBLE;
    body.setVisibility(open ? View.GONE : View.VISIBLE);
    header.setText(title + (open ? " (tap to expand)" : " (tap to collapse)"));
}
```

#### Network Security
🌐 Analyzes the current network connection (Wi-Fi, Cellular, VPN) and evaluates security signals such as Private DNS and the presence of a proxy or captive portal to calculate a risk score.
<p align="center">
  <img src="https://github.com/user-attachments/assets/9dc19ee0-435f-4f40-88b4-9635b360ecd4"
       alt="image"
       height="600" />
</p>

```java
private static SecurityReport buildSecurityReport(Context ctx) {
    SecurityReport r = new SecurityReport();
    int risk = 0;
    ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    Network active = cm.getActiveNetwork();
    NetworkCapabilities caps = active != null ? cm.getNetworkCapabilities(active) : null;
    // ... (logic evaluates network capabilities, VPN, proxy, etc.) ...
    boolean vpn = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
    if (!vpn) risk += 10;
    // ...
    r.riskScore = risk;
    return r;
}
```

#### Cyber Quiz
❓ An interactive quiz that loads questions from a local JSON file to test the user's knowledge. The score is then saved to Firebase to track progress.
<p align="center">
  <img src="https://github.com/user-attachments/assets/f9e79bf7-12bb-4258-9ab0-9bd8b70d76b3"
       alt="image"
       height="600" />
</p>

```java
private void onOptionSelected(int selectedIndex) {
    if (answered) return;
    Question q = runQuestions.get(index);
    answered = true;
    boolean correct = selectedIndex == q.answerIndex;
    if (correct) score++;
    feedbackCard.setVisibility(View.VISIBLE);
    tvFeedbackTitle.setText(correct ? "Correct ✅" : "Incorrect ❌");
    // ... Display explanation and enable next button ...
    btnNext.setEnabled(true);
}
```

#### Password Generator
🔑 Generates strong and random passwords. The user can customize the length and character sets (uppercase, lowercase, numbers, symbols).
<p align="center">
  <img src="https://github.com/user-attachments/assets/3cb72336-1e79-4ef1-a0dc-5456dea64f72"
       alt="image"
       height="600" />
</p>

```java
private void generateAndDisplay() {
    int length = getSelectedLength();
    String pool = buildPool();
    if (pool.isEmpty()) return; // Must select at least one character set

    List<Character> chars = new ArrayList<>();
    // ... (Ensures at least one character from each selected set is included) ...

    while (chars.size() < length) {
        chars.add(randomCharFrom(pool));
    }

    Collections.shuffle(chars);

    StringBuilder sb = new StringBuilder();
    for (char c : chars) sb.append(c);

    tvPassword.setText(sb.toString());
    updateStrengthLabel(length);
}
```

### 5.2. User Profile and Editing

The profile screen displays user information and allows them to edit it. The `ProfileViewModel` handles the logic for loading and updating user data.

<p align="center">
  <img src="https://github.com/user-attachments/assets/6d02d71a-16b7-4e5c-b90b-6686c52a1c88"
       alt="image"
       height="600" />
</p>

The code below toggles edit mode and saves changes.

```java
private void setupClickListeners() {
    binding.profileTvEdit.setOnClickListener(v -> toggleEditMode());
    binding.profileTvSave.setOnClickListener(v -> {
        viewModel.saveProfile(
                binding.profileEtFullName.getText().toString(),
                binding.profileEtPhone.getText().toString()
        );
    });
}
```

## 6. Other Navigation Drawer Features

*   **Rate App**: Redirects the user to the Google Play Store.
*   **Share App**: Opens a share dialog to share a link to the app.
*   **About**: Displays information about the app.
*   **Privacy Policy**: Shows the app's privacy policy.

---

## License

This project is licensed under the [MIT License](LICENSE.md) — see the LICENSE file for details.

## Contributing

Contributions are welcome! For any suggestions or improvements, please open an issue or submit a pull request.

1.  Fork the repository.
2.  Create a feature branch (`git checkout -b feature/NewFeature`).
3.  Commit your changes (`git commit -m 'Add a new feature'`).
4.  Push to the branch (`git push origin feature/NewFeature`).
5.  Open a Pull Request.
