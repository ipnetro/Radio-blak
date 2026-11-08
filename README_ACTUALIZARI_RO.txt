Actualizări Radio Ip-NeT

API GitHub: https://api.github.com/repos/ipnetro/Radio.Red-Black/releases/latest
Release: https://github.com/ipnetro/Radio.Red-Black/releases/latest
Numele asset-ului APK: Radio-Ip-NeT.apk

Compararea versiunilor folosește componentele numerice. Pentru 1.1.38 instalat și 1.1.38 publicat, aplicația afișează „Aplicația este la zi — nu trebuie actualizată” și nu afișează dialog de descărcare.

Dacă versiunea publicată este mai nouă, aplicația caută asset-ul .apk din lista assets. Nu folosește html_url ca URL de descărcare. Dacă APK-ul lipsește, afișează un mesaj explicit.

Now Playing (corectie sursa):
- Aplicatia interogheaza API-ul public AzuraCast /api/nowplaying/{station_shortcode} la 15 secunde in timpul redarii.
- Titlul si artistul melodiei curente actualizeaza zona ACUM ASCULTI si metadatele MediaSession/notificarii.
- Coperta furnizata de AzuraCast este atasata metadatelor media.
