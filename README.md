# react-native-wifi-tools

This project has been created from [react-native-iot-wifi](https://github.com/tadasr/react-native-iot-wifi) the work of Nectar Sun.

## Getting started

`$ npm install react-native-wifi-tools --save`

### Mostly automatic installation

`$ react-native link react-native-wifi-tools`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-wifi-tools` and add `WifiTools.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libWifiTools.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.reactlibrary.WifiToolsPackage;` to the imports at the top of the file
  - Add `new WifiToolsPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-wifi-tools'
  	project(':react-native-wifi-tools').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-wifi-tools/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-wifi-tools')
  	```


Permissions required:
```
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
```


## Usage
```javascript
import WifiTools from 'react-native-wifi-tools';

// TODO: What to do with the module?
WifiTools;
```
