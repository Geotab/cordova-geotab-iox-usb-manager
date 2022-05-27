## Archived
This Cordova plugin is now archived and is no longer supported by Geotab.

# cordova-geotab-iox-usb-manager

Plugin for data exchange between Android device and Geotab's GO device via IOX-USB wire

To use this plugin you need just call the plugin's method `setCallback(cb)` with `cb` function. 
This function will receive one parameter - data object from Java code. This data object has `myData` property, which is a string representing engine data that is read from GO device via IOX-USB cable. To use in the application need to make `JSON.parse(data.myData)`
