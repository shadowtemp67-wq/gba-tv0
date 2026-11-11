# GBA TV
1. `app/cores/fetch-cores.sh`  (downloads the mGBA libretro core)
2. Open in Android Studio (JDK 17) or run `gradle wrapper && ./gradlew assembleDebug`
3. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
4. Add ROMs: use "+ Add ROM" tile, or
   `adb push game.gba /sdcard/Android/data/com.example.gbatv/files/roms/`
Controls: D-pad/OK on remote; Xbox pad: B/Y=A, A/X=B, LB/RB=L/R. Back or Guide = menu.
Leaving a game auto-saves; relaunching resumes. Use only ROMs you own.
