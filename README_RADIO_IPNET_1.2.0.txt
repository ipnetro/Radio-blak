Radio Ip-NeT 1.2.0 - proiect Android

Functii incluse:
- Favorite persistente (SharedPreferences)
- Istoric cu ultimele 20 de posturi ascultate
- Controale media prin MediaSessionService (notificare/ecran blocat, suport Android)
- Teme de accent: rosu, albastru, verde, mov si portocaliu
- Afisarea metadatelor curente ale playerului atunci cand streamul le furnizeaza
- Egalizator Android: reglaj de nivel pentru banda joasa, prin sesiunea audio ExoPlayer; disponibilitatea depinde de telefon

Pastrate:
- applicationId si namespace: ro.ipnet
- Cele trei posturi si URL-urile lor originale
- Verificarea actualizarilor GitHub din Radio.Red-Black
- Workflow-ul existent pentru build si Release

Important:
- Proiectul trebuie compilat in GitHub Actions inainte de publicarea APK-ului.
- Pentru actualizare peste instalatia existenta, folositi aceeasi cheie de semnare ca versiunile anterioare.
- Nu schimbati applicationId si nu stergeti secretele GitHub de semnare.
