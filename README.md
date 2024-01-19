# GeocachingApp

## Overview
This repository includes the Andorid Studio project of the "GeocachingApp".
The app facilitates geocache hiding and searching. 
In the "Hide Phase" users can select geocaches through QR code scans or dropdown lists, storing their coordinates in a database. 
These hidden caches are visually represented on an responsive online map. 
The "Search Phase" employs GPS to (optionally) trigger voice announcements upon approaching geocaches within a configurable radius. 
Scanning QR codes marks the cache as found, updating the map accordingly. (In debug mode selecting checkboxes suffices to mark geocaches as found.)
The app includes a configuration menu for radius adjustment, a list of found and outstanding geocaches, and the ability to save the database content as a GPX-formatted XML file locally. 
The QR-Codes for the geocaches can be obtained from within the App.


The App is split into three Layouts:

### Map
Here the current position is displayed.
In the "Hide Phase" the icons of the hidden geocaches are displayed and in the "Search Phase" the icons of the found geocaches are displayed here.

### Caches
A list of all geocaches.
Their respective status as "hidden" or "found" is marked in the corresponding checkboxs.

### Settings
1. Switch between debug and QR-Code-Mode as a way to select geocaches
2. Enable Audio Feedback when near a geocaches
3. Set the Audio Feedback radius in meters
4. Export the geocaches as a GPX file
5. Save a PNG of the QR-Codes


## Building the APK

Follow these steps to build the APK using Android Studio:

1. Clone the repository to your local environment.
2. Launch Android Studio.
3. Select "Open an existing Android Studio project".
4. Browse to the repository's location on your machine and select it.
5. Android Studio will then load and sync the project.
6. Once the project loads, click on the 'Build' option in the top menu.
7. Navigate to 'Build Bundle(s) / APK(s)' and then select 'Build APK'.
8. After the build process completes, the APK will be generated and can be found in the project's `\...\GeocachingApp\app\build\outputs\apk` directory, ready for deployment or testing.


## Language

The Language of the App is German.


## Permissions

The App needs Location and Camera Permissions.


## Libraries

This App was built using the [Mapbox Java SDK](https://docs.mapbox.com/android/java/guides/), [Mapbox Maps SDK](https://docs.mapbox.com/android/maps/guides/), [Mapbox Annotation Plugin](https://docs.mapbox.com/android/legacy/plugins/guides/annotation/) and [JourneyApps ZXING](https://github.com/journeyapps/zxing-android-embedded) (see `\...\GeocachingApp\app\build.gradle`).


## License

All novel code in this App is completely free to use.
