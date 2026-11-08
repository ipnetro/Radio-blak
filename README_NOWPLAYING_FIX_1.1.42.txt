Radio Ip-NeT 1.1.42 - corectie Now Playing

- Eliminata inlocuirea MediaItem-ului activ la fiecare melodie noua.
- Eliminat seekTo() executat dupa actualizarea metadatelor, care putea repozitiona/reporni streamul live.
- UI-ul continua sa citeasca AzuraCast Now Playing imediat la schimbarea postului si apoi la 2 secunde in timpul redarii.
- Actualizat versionCode/versionName la 1.1.42.

Nota: metadatele UI sunt actualizate din API. Notificarea/ecranul blocat primesc metadatele streamului prin Media3; nu fortam schimbarea MediaItem-ului deoarece ar putea intrerupe streamul live.
