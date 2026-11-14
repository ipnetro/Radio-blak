# Radio Ip-NeT

**Muzica preferată, live, direct pe Android.**

Radio Ip-NeT este o aplicație Android pentru ascultarea posturilor Radio Angel și Radio Tequila Club. Alege un post, pornește redarea și continuă să asculți în fundal, inclusiv cu comenzile media din notificare sau de pe ecranul blocat.

## Despre aplicație

| Element | Valoare |
|---|---|
| Nume | **Radio Ip-NeT** |
| Application ID | `ro.ipnet` |
| Android minim | 6.0 (API 23) |
| Versiunea sursei de pe `main` | 1.1.46 |
| Ultima versiune publicată | 1.1.46 |

Versiunea sursei și ultimul APK publicat sunt ambele **1.1.46**. Descarcă [APK-ul oficial](https://github.com/ipnetro/Radio.Red-Black/releases/latest).

## Posturi radio

Configurația implicită conține:

- Radio Angel Hip-Hop
- Radio Angel Dance
- Radio Angel Pop
- Radio Tequila Club

Aplicația descarcă și configurația posturilor de la serviciul Radio Ip-NeT, deci lista se poate schimba fără o versiune nouă a aplicației. Redarea și informațiile despre melodii depind de conexiunea la internet și de serviciile posturilor.

## Funcționalități

### Ascultare și player

- Redare audio live și continuarea redării în fundal.
- Comenzi din aplicație, notificare, ecranul blocat și widget.
- Reglarea volumului și oprirea sunetului.
- Afișarea melodiei și a imaginii albumului când postul le furnizează.

### Posturile tale

- Salvarea posturilor favorite.
- Istoric local cu ultimele 20 de posturi ascultate.
- Revenire rapidă la postul selectat anterior.

### Radio Angel Hip-Hop

- Trimiterea unei cereri de melodie, dacă funcția este disponibilă.
- Consultarea istoricului melodiilor difuzate, dacă serviciul îl furnizează.

### Personalizare și actualizări

- Culori de accent: roșu, albastru, verde, mov și portocaliu.
- Verificarea configurației și versiunilor noi prin `update.ip-net.ro`.
- Descărcarea APK-ului indicat de serviciul de actualizare; Android cere confirmarea instalării.
- Egalizator Android, dacă telefonul îl suportă.

## Actualizări automate

Aplicația citește versiunea disponibilă de la `update.ip-net.ro/version.json`. Fișierul de pe server trebuie actualizat după publicarea unei versiuni; valorile necesare pentru Release-ul **1.1.46** sunt în [instrucțiunile updaterului](update-server/README.md). Până la actualizarea serverului, utilizatorii versiunii 1.1.45 pot descărca manual APK-ul nou din Releases.

## Instalare

Descarcă APK-ul oficial din [GitHub Releases](https://github.com/ipnetro/Radio.Red-Black/releases/latest). Pentru a instala o versiune peste cea existentă, păstrează același Application ID și aceeași cheie de semnare.

## Compilare și publicare

Proiectul folosește Kotlin, Jetpack Compose și AndroidX Media3. Pentru compilare sunt necesare JDK 17 și Android SDK 35.

Compilează local varianta Debug:

```bash
gradle assembleDebug
```

GitHub Actions compilează versiunea Debug pe ramura `main`. Un Release semnat se publică printr-un tag de forma `vMAJOR.MINOR.PATCH`. Cheia de semnare este păstrată în secretele GitHub Actions; nu încărca fișierul keystore sau parolele în repository.

Consultă [nota de confidențialitate](PRIVACY.md) și [instrucțiunile de securitate](SECURITY.md).

## Licență

Aplicația și codul sursă sunt proprietare. Citește [LICENSE](LICENSE). Bibliotecile și componentele terțe rămân sub licențele lor proprii.

---

**Radio Ip-NeT**  
**FEEL THE MUSIC**
