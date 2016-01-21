# Taiwan WiFi
Android app to display iTaiwan WiFi hotspots using an offline map.  Most free WiFi apps on the Play Store use Google Maps requiring a internet connection making them useless in many cases. 

This app uses an bundled map file to render maps offline. 

### Building
Place "taiwan.map" (downloaded from mapsforge) into the assets directory before buidling in Android Studio.

### Todo
- Provide information screen about registering for iTaiwan WiFi. 
- Chinese language support. 
- Ability to update list of HotSpots from online. 
- Map is an asset in the APK, but needs to be extracted for random access.  This means essentially storing the map twice. 
