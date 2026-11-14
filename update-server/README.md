# Actualizarea versiunii pentru updater

Aplicația Radio Ip-NeT citește versiunea disponibilă din:

`https://update.ip-net.ro/version.json`

La verificarea din 14 noiembrie 2026, endpoint-ul public raportează versiunea 1.1.46 și codul 1001046, iar linkul APK indică ultimul GitHub Release. Fișierul version.json din repository reflectă această configurație. Serverul de actualizări este administrat separat și trebuie reverificat după fiecare publicare.

Fișierul [version.json](version.json) din acest director conține valorile pentru versiunea publicată curent. Încarcă-l pe serverul care găzduiește `update.ip-net.ro`, înlocuind fișierul `version.json`, apoi verifică URL-ul public și asigură-te că afișează 1.1.46 / 1001046.

`apkUrl` folosește linkul permanent către APK-ul ultimului GitHub Release. La următoarea versiune, actualizează `latestVersion`, `versionCode` și `message` și publică Release-ul înainte de a înlocui acest fișier.
