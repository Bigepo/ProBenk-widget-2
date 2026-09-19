PROBENK MOBILE WIDGET v1 - ANDROID

Dette er et vanlig Android Studio-prosjekt i Java, uten tredjeparts runtime-biblioteker.

BYGG
1. Åpne mappen android/ProBenkWidget i Android Studio.
2. La Android Studio synkronisere Gradle.
3. Bygg appen (Build > Build APK(s)).
4. Installer APK på Android-telefonen.
5. Åpne appen og koble den med engangskoden fra /admin/mobile-widget.php.
6. Langtrykk på hjemskjermen > Widgets > ProBenk.

WIDGETEN
- Oppdateres av Android periodisk (30 min), samt manuelt med ↻.
- 1 / 3 / 5 rader avhengig av widgetens høyde.
- Viser dagens oppmåling, montering, service, bestilling og forventet levering.
- Hvis dagen er tom, viser den neste planlagte punkt(er).
- Trykk på rad åpner prosjektet i nettleseren.
- Trykk på header åpner ProBenk-kalenderen.

PORTALADRESSE
Standard er https://probenk.no/admin/ og kan endres i appen før paring.
Kun HTTPS godtas.

MERK
Kildepakken inneholder ikke ferdig signert APK. Den må bygges/signeres i Android Studio.
