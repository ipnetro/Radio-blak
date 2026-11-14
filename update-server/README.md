# Actualizarea versiunii pentru updater

Aplicația Radio Ip-NeT citește versiunea disponibilă din:

`https://update.ip-net.ro/version.json`

În momentul publicării v1.1.46, endpoint-ul public încă raportează versiunea 1.1.45 și codul 1001045. Utilizatorii care au instalată versiunea 1.1.45 nu vor primi automat oferta pentru v1.1.46 până când fișierul de pe serverul de actualizări nu este actualizat.

Fișierul [version.json](version.json) din acest director conține valorile pentru versiunea publicată curent. Încarcă-l pe serverul care găzduiește `update.ip-net.ro`, înlocuind fișierul `version.json`, apoi verifică URL-ul public și asigură-te că afișează 1.1.46 / 1001046.

`apkUrl` folosește linkul permanent către APK-ul ultimului GitHub Release. La următoarea versiune, actualizează `latestVersion`, `versionCode` și `message` și publică Release-ul înainte de a înlocui acest fișier.
