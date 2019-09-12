'use strict';

import { NativeModules } from 'react-native';

function connect(...args) {
  if (args.length < 2 || args.length > 3) {
    throw new TypeError('invalid arguments');
  } else if (args.length === 2) {
    NativeModules.WifiTools.connect(args[0], false, args[1]);
  } else {
    NativeModules.WifiTools.connect(...args);
  }
}

function connectSecure(...args) {
  if (args.length < 4 || args.length > 5) {
    throw new TypeError('invalid arguments');
  } else if (args.length === 4) {
    NativeModules.WifiTools.connectSecure(args[0], args[1], args[2], false, args[3]);
  } else {
    NativeModules.WifiTools.connectSecure(...args);
  }
}

function removeSSID(...args) {
  if (args.length < 2 || args.length > 3) {
    throw new TypeError('invalid arguments');
  } else if (args.length === 2) {
    NativeModules.WifiTools.removeSSID(args[0], false, args[1]);
  } else {
    NativeModules.WifiTools.removeSSID(...args);
  }
}

function getSSID(...args) {
  NativeModules.WifiTools.getSSID(...args);
}

function isApiAvailable(...args) {
  NativeModules.WifiTools.isApiAvailable(...args);
}

module.exports = {
  connect: connect,
  connectSecure: connectSecure,
  getSSID: getSSID,
  isApiAvailable: isApiAvailable,
  removeSSID: removeSSID,
};

/**
 * (un)bindNetwork only affects Android
 * isApiAvailable(Callback callback)
 * connect(String ssid, Boolean bindNetwork = false, Callback callback)
 * todo: connectSecure(String ssid, String passphrase, Boolean isWEP, Boolean bindNetwork = false, Callback callback)
 * removeSSID(String ssid, Boolean unbindNetwork = false, Callback callback)
 * getSSID(Callback callback)
 */
