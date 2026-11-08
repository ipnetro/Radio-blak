Radio Ip-NeT — proiect Android

Versiune sursa: 1.1.39 (versionCode 1001039)
Application ID / namespace: ro.ipnet
Repository actualizari: ipnetro/Radio.Red-Black

Proiectul conține aplicația Android, cele trei posturi radio și workflow-ul GitHub Actions.

Publicare actualizare:
1. Încarcă TOT conținutul acestei arhive în rădăcina repository-ului Radio.Red-Black (nu încărca arhiva ZIP ca fișier în repository).
2. Păstrează secretele GitHub existente: KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
3. Creează și împinge un tag nou, de exemplu v1.1.39.
4. În GitHub Actions, așteaptă ca jobul build-release să se termine cu succes.
5. În Release trebuie să apară asset-ul Radio-Ip-NeT.apk.

Aplicația verifică ultima versiune la pornire și din meniul hamburger. Dacă versiunea instalată este egală sau mai nouă decât cea publicată, afișează că aplicația este la zi. Dacă există o versiune mai nouă și Release-ul conține APK, descarcă APK-ul și cere confirmarea standard Android pentru instalare.

IMPORTANT: Pentru actualizări peste instalarea existentă, păstrează applicationId ro.ipnet și aceeași cheie de semnare. Android va cere confirmarea instalării.
