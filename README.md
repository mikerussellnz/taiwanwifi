# Taiwan WiFi
Android app to display iTaiwan WiFi hotspots using an offline map.  Most free WiFi apps on the Play Store use Google Maps requiring a internet connection making them useless in many cases. 

This app uses an bundled map file to render maps offline. 

### Building
Place "taiwan.map" (downloaded from mapsforge) into the assets directory before buidling in Android Studio.

### Todo
- Improve clustering algorithm. Current algorithm doesn't always produce nice clusters.
- Mapsforge uses a boundingbox contains check on each item to decide if it should be rendered.  Clustering uses a quadtree which could be used to add and remove items as the view is panned. 
- Pan map when popup is displayed so that whole popup can be seen. 
- Provide information screen about registering for iTaiwan WiFi. 
- Chinese language support. 
- Ability to update list of HotSpots from online. 
- Map is an asset in the APK, but needs to be extracted for random access.  This means essentially storing the map twice. 
