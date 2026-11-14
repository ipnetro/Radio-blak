# Radio Ip-NeT

**Muzica preferată, live, direct pe Android.**

Radio Ip-NeT este o aplicație Android pentru ascultarea posturilor Radio Angel și Radio Tequila Club. Alege un post, pornește redarea și continuă să asculți în fundal, inclusiv cu comenzile media din notificare sau de pe ecranul blocat.

## Despre aplicație

| Element | Valoare |
|---|---|
| Nume | **Radio Ip-NeT** |
| Application ID | `ro.ipnet` |
| Platformă | Android |
| Android minim | 6.0 (API 23) |
| Versiunea sursei | 1.1.46 |

## Posturi radio

- Radio Angel Hip-Hop
- Radio Angel Dance
- Radio Angel Pop
- Radio Tequila Club

Disponibilitatea fluxurilor radio și a informațiilor despre melodii depinde de serviciile fiecărui post și de conexiunea la internet.

## Funcționalități

### Ascultare și player

- Redare audio live și continuarea redării în fundal.
- Comenzi de redare din aplicație, notificare și ecranul blocat.
- Reglarea volumului și oprirea sunetului.
- Afișarea melodiei și a imaginii albumului atunci când postul furnizează aceste informații.

### Posturile tale

- Salvarea posturilor favorite.
- Istoric local cu ultimele 20 de posturi ascultate.
- Revenire rapidă la postul selectat anterior.

### Funcții Radio Angel Hip-Hop

- Trimiterea unei cereri de melodie, dacă funcția este disponibilă.
- Consultarea istoricului melodiilor difuzate, dacă serviciul îl furnizează.

### Personalizare și actualizări

- Alegerea culorii de accent: roșu, albastru, verde, mov sau portocaliu.
- Verificarea actualizărilor și descărcarea versiunilor publicate, cu confirmarea standard de instalare Android.
- Egalizatorul Android este disponibil dacă telefonul îl suportă.

## Instalare

Descarcă APK-ul oficial din [GitHub Releases](https://github.com/ipnetro/Radio.Red-Black/releases). Android poate cere să confirmi instalarea unei aplicații descărcate din afara magazinului.

Pentru actualizări peste o versiune instalată, APK-ul trebuie semnat cu aceeași cheie și să păstreze același Application ID.

## Compilarea proiectului

Proiectul folosește Kotlin, Jetpack Compose și AndroidX Media3. Pentru compilare sunt necesare JDK 17 și Android SDK 35.

Comanda de compilare a variantei Debug:

```bash
gradle assembleDebug
```

Versiunile publicate sunt generate prin workflow-ul GitHub Actions. Cheile de semnare trebuie păstrate private și configurate ca secrete în GitHub Actions.

## Licență

Aplicația și codul sursă sunt proprietare. Consultă fișierul [LICENSE](LICENSE). Bibliotecile și componentele terțe rămân sub licențele lor proprii.

---

**Radio Ip-NeT**  
**FEEL THE MUSIC**
