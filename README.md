# Radio Ip-NeT

## 🇬🇧


**Your favorite music live on Android.**

Radio Ip-NeT is an Android app for listening to Radio Angel and Radio Tequila Club. Choose a station, start playback, and keep listening in the background, including with media controls in the notification or on the lock screen.

### About the app

| Field | Value |
|---|---|
| Name | **Radio Ip-NeT** |
| Application ID | `ro.ipnet` |
| Minimum Android | 6.0 (API 23) |
| Source version on `main` | 1.1.47 |
| Latest published version | 1.1.47 |

The source version and latest published APK are both **1.1.47**. Download the [official APK](https://github.com/ipnetro/Radio.Red-Black/releases/latest).

### Radio stations

The default configuration includes:
- Radio Angel Hip-Hop
- Radio Angel Dance
- Radio Angel Pop
- Radio Tequila Club

The app downloads its station configuration from the Radio Ip-NeT service, so the station list can change without a new app version. Playback and song information depend on your internet connection and the stations' services.

### Features

**Listening and player**
- Listen to live audio and continue playback in the background.
- Control playback from the app, notification, lock screen, and widget.
- Adjust the volume and mute the sound.
- View the song and album artwork when provided by the station.

**Your stations**
- Save favorite stations.
- Keep a local history of the last 20 stations played.
- Quickly return to the previously selected station.

**Radio Angel Hip-Hop**
- Send a song request when this feature is available.
- View the broadcast song history when provided by the service.

**Customization and updates**
- Accent colors: red, blue, green, purple, and orange.
- Check the configuration and new versions through `update.ip-net.ro`.
- Download the APK provided by the update service; Android asks you to confirm the installation.
- Android equalizer, if supported by your phone.
- Choose Romanian or English for the app interface; your choice is saved on the phone.

### Automatic updates

The app reads the available version from `update.ip-net.ro/version.json`. The server file must be updated after a release; the values required for Release **1.1.47** are in the [updater instructions](update-server/README.md). Until the server is updated, users on version 1.1.45 can manually download the new APK from Releases.

### Installation

Download the official APK from [GitHub Releases](https://github.com/ipnetro/Radio.Red-Black/releases/latest). To install an update over the existing app, keep the same Application ID and signing key.

### Build and release

The project uses Kotlin, Jetpack Compose, and AndroidX Media3. Building requires JDK 17 and Android SDK 35.

Build the Debug version locally:

```bash
gradle assembleDebug
```

GitHub Actions builds the Debug version on the `main` branch. A signed Release is published using a tag in the form `vMAJOR.MINOR.PATCH`. The signing key is stored in GitHub Actions secrets; do not upload the keystore file or passwords to the repository.

Read the [privacy notice](PRIVACY.md) and [security instructions](SECURITY.md).

### License

The app and source code are proprietary. Read [LICENSE](LICENSE). Third-party libraries and components remain under their respective licenses.

### Feedback and ideas

Have an idea or found a problem? Open a [new issue](https://github.com/ipnetro/Radio.Red-Black/issues/new) and choose the appropriate form. Read the [feedback and contribution guide](.github/CONTRIBUTING.md). Please do not post passwords, API keys, or private information.

---

**Radio Ip-NeT**  
**FEEL THE MUSIC**


---

## 🇷🇴

**Muzica preferată, live, direct pe Android.**

Radio Ip-NeT este o aplicație Android pentru ascultarea posturilor Radio Angel și Radio Tequila Club. Alege un post, pornește redarea și continuă să asculți în fundal, inclusiv cu comenzile media din notificare sau de pe ecranul blocat.

### Despre aplicație

| Element | Valoare |
|---|---|
| Nume | **Radio Ip-NeT** |
| Application ID | `ro.ipnet` |
| Android minim | 6.0 (API 23) |
| Versiunea sursei de pe `main` | 1.1.47 |
| Ultima versiune publicată | 1.1.47 |

Versiunea sursei și ultimul APK publicat sunt ambele **1.1.47**. Descarcă [APK-ul oficial](https://github.com/ipnetro/Radio.Red-Black/releases/latest).

### Posturi radio

Configurația implicită conține:
- Radio Angel Hip-Hop
- Radio Angel Dance
- Radio Angel Pop
- Radio Tequila Club

Aplicația descarcă și configurația posturilor de la serviciul Radio Ip-NeT, deci lista se poate schimba fără o versiune nouă a aplicației. Redarea și informațiile despre melodii depind de conexiunea la internet și de serviciile posturilor.

### Funcționalități

**Ascultare și player**
- Redare audio live și continuarea redării în fundal.
- Comenzi din aplicație, notificare, ecranul blocat și widget.
- Reglarea volumului și oprirea sunetului.
- Afișarea melodiei și a imaginii albumului când postul le furnizează.

**Posturile tale**
- Salvarea posturilor favorite.
- Istoric local cu ultimele 20 de posturi ascultate.
- Revenire rapidă la postul selectat anterior.

**Radio Angel Hip-Hop**
- Trimiterea unei cereri de melodie, dacă funcția este disponibilă.
- Consultarea istoricului melodiilor difuzate, dacă serviciul îl furnizează.

**Personalizare și actualizări**
- Culori de accent: roșu, albastru, verde, mov și portocaliu.
- Verificarea configurației și versiunilor noi prin `update.ip-net.ro`.
- Descărcarea APK-ului indicat de serviciul de actualizare; Android cere confirmarea instalării.
- Egalizator Android, dacă telefonul îl suportă.
- Alege limba interfeței, română sau engleză; alegerea se salvează pe telefon.

### Actualizări automate

Aplicația citește versiunea disponibilă de la `update.ip-net.ro/version.json`. Fișierul de pe server trebuie actualizat după publicarea unei versiuni; valorile necesare pentru Release-ul **1.1.47** sunt în [instrucțiunile updaterului](update-server/README.md). Până la actualizarea serverului, utilizatorii versiunii 1.1.45 pot descărca manual APK-ul nou din Releases.

### Instalare

Descarcă APK-ul oficial din [GitHub Releases](https://github.com/ipnetro/Radio.Red-Black/releases/latest). Pentru a instala o versiune peste cea existentă, păstrează același Application ID și aceeași cheie de semnare.

### Compilare și publicare

Proiectul folosește Kotlin, Jetpack Compose și AndroidX Media3. Pentru compilare sunt necesare JDK 17 și Android SDK 35.

Compilează local varianta Debug:

```bash
gradle assembleDebug
```

GitHub Actions compilează versiunea Debug pe ramura `main`. Un Release semnat se publică printr-un tag de forma `vMAJOR.MINOR.PATCH`. Cheia de semnare este păstrată în secretele GitHub Actions; nu încărca fișierul keystore sau parolele în repository.

Consultă [nota de confidențialitate](PRIVACY.md) și [instrucțiunile de securitate](SECURITY.md).

### Licență

Aplicația și codul sursă sunt proprietare. Citește [LICENSE](LICENSE). Bibliotecile și componentele terțe rămân sub licențele lor proprii.

### Feedback și idei

Ai o sugestie sau ai găsit o problemă? Deschide un [issue nou](https://github.com/ipnetro/Radio.Red-Black/issues/new) și alege formularul potrivit. Citește [ghidul de feedback și contribuții](.github/CONTRIBUTING.md). Te rugăm să nu publici parole, chei API sau date private.

---

**Radio Ip-NeT**  
**FEEL THE MUSIC**
