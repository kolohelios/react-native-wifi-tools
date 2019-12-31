package com.kolohelios.reactnative.wifitools;

import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

import android.annotation.TargetApi;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiNetworkSpecifier;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.List;
import javax.annotation.Nullable;


class FailureCodes {
    static int SYSTEM_ADDED_CONFIG_EXISTS = 1;
    static int FAILED_TO_CONNECT = 2;
    static int FAILED_TO_ADD_CONFIG = 3;
    static int FAILED_TO_BIND_CONFIG = 4;
    static int TIMED_OUT_CONNECTING = 5;
    static int UNABLE_TO_DISCONNECT = 6;
}

public class WifiToolsModule extends ReactContextBaseJavaModule {
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private ReactApplicationContext context;

    // Sending events is good, so long as we don't spam the bridge
    private void sendEvent(ReactContext reactContext,
                        String eventName,
                        String message) {
        WritableMap params = Arguments.createMap();
        params.putString("eventProperty", message);

        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    public WifiToolsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        wifiManager = (WifiManager) getReactApplicationContext().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getReactApplicationContext().getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        context = getReactApplicationContext();
    }

    private String errorFromCode(int errorCode) {
        return "ErrorCode: " + errorCode;
    }

    @Override
    public String getName() {
        return "WifiTools";
    }

    @ReactMethod
    public void isApiAvailable(final Callback callback) {
        callback.invoke(true);
    }

    @ReactMethod
    public void connect(String ssid, Boolean bindNetwork, Callback callback) {
        connectSecure(ssid, "", false, bindNetwork, callback);
    }

    @ReactMethod
    public void connectSecure(final String ssid, final String passphrase, final Boolean isWEP,
        final Boolean bindNetwork, final Callback callback) {
        new Thread(new Runnable() {
            public void run() {
                connectToWifi(ssid, passphrase, isWEP, bindNetwork, callback);
            }
        }).start();
    }

    private void connectToWifi(String ssid, String passphrase, Boolean isWEP, Boolean bindNetwork, Callback callback) {
        sendEvent(context, "WifiEvents", "starting connection to: " + ssid + " with passphrase " + passphrase);
        sendEvent(context, "WifiEvents", "SDK version: " + Build.VERSION.SDK_INT);

        if (!removeSSID(ssid)) {
            callback.invoke(errorFromCode(FailureCodes.SYSTEM_ADDED_CONFIG_EXISTS));
            sendEvent(context, "WifiEvents", "unable to remove SSID");
            return;
        }

        if (Build.VERSION.SDK_INT <= 28) {
            // callback.invoke("Not supported on Android Q");
            // return;
            WifiConfiguration configuration = createWifiConfiguration(ssid, passphrase, isWEP);
            int networkId = wifiManager.addNetwork(configuration);
            sendEvent(context, "WifiEvents", "networkId: " + networkId);

            if (networkId != -1) {
                // Enable it so that android can connect
                boolean disconnected = wifiManager.disconnect();

                if (disconnected) {
                    sendEvent(context, "WifiEvents", "disconnected");
                } else {
                    sendEvent(context, "WifiEvents", "unable to disconnect");
                }

                boolean success =  wifiManager.enableNetwork(networkId, true);
                if (!success) {
                    sendEvent(context, "WifiEvents", "unsuccessful enabling network with id: " + networkId);
                    callback.invoke(errorFromCode(FailureCodes.FAILED_TO_ADD_CONFIG));
                    return;
                } else {
                    sendEvent(context, "WifiEvents", "successfully enabled network with id: " + networkId);
                }
                success = wifiManager.reconnect();
                if (!success) {
                    sendEvent(context, "WifiEvents", "unsuccessful reconnecting");
                    callback.invoke(errorFromCode(FailureCodes.FAILED_TO_CONNECT));
                    return;
                } else {
                    sendEvent(context, "WifiEvents", "successfully reconnected");
                }
                boolean connected = pollForValidSsid(10, ssid);
                if (!connected) {
                    if (!disconnected) {
                        callback.invoke(errorFromCode(FailureCodes.UNABLE_TO_DISCONNECT));
                    } else {
                        callback.invoke(errorFromCode(FailureCodes.TIMED_OUT_CONNECTING));
                    }
                    return;
                }
                if (!bindNetwork) {
                    callback.invoke();
                    return;
                }
                try {
                    bindToNetwork(ssid, callback);
                } catch (Exception e) {
                    Log.d("WifiTools", "Failed to bind to Wifi: " + ssid);
                    callback.invoke();
                }
            } else {
                callback.invoke(errorFromCode(FailureCodes.FAILED_TO_ADD_CONFIG));
            }
        } else {
            WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
            builder.setSsid(ssid);
            builder.setWpa2Passphrase(passphrase);

            WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
            // networkId = wifiManager.addNetwork(wifiNetworkSpecifier);

            NetworkRequest.Builder networkRequestBuilder1 = new NetworkRequest.Builder();
            networkRequestBuilder1.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            networkRequestBuilder1.setNetworkSpecifier(wifiNetworkSpecifier);

            NetworkRequest nr = networkRequestBuilder1.build();
            final ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            ConnectivityManager.NetworkCallback networkCallback = new
                ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    Log.d("Hello", "onAvailable:" + network);
                    cm.bindProcessToNetwork(network);
                }
            };
            cm.requestNetwork(nr, networkCallback);
        }


    }

    private WifiConfiguration createWifiConfiguration(String ssid, String passphrase, Boolean isWEP) {
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = String.format("\"%s\"", ssid);

        if (passphrase.equals("")) {
            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else if (isWEP) {
            configuration.wepKeys[0] = "\"" + passphrase + "\"";
            configuration.wepTxKeyIndex = 0;
            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        } else { // WPA/WPA2
            configuration.preSharedKey = "\"" + passphrase + "\"";
        }

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        return configuration;
    }

    private boolean pollForValidSsid(int maxSeconds, String expectedSSID) {
      try {
        for (int i = 0; i < maxSeconds; i++) {
          String ssid = this.getWifiSSID();
          if (ssid != null && ssid.equalsIgnoreCase(expectedSSID)) {
            return true;
          }
          Thread.sleep(1000);
        }
      } catch (InterruptedException e) {
        return false;
      }
      return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void bindToNetwork(final String ssid, final Callback callback) {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {

                private boolean bound = false;

                @Override
                public void onAvailable(Network network) {
                    String offeredSSID = getWifiSSID();

                    if (!bound && offeredSSID.equals(ssid)) {
                        try {
                            bindProcessToNetwork(network);
                            bound = true;
                            callback.invoke();
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    callback.invoke(errorFromCode(FailureCodes.FAILED_TO_BIND_CONFIG));
                }

                @Override
                public void onLost(Network network) {
                    if (bound) {
                        bindProcessToNetwork(null);
                        connectivityManager.unregisterNetworkCallback(this);
                    }
                }
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void bindProcessToNetwork(final Network network) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.bindProcessToNetwork(network);
        } else {
            ConnectivityManager.setProcessDefaultNetwork(network);
        }
    }

    @ReactMethod
    public void removeSSID(String ssid, Boolean unbind, Callback callback) {
        if (!removeSSID(ssid)) {
            callback.invoke(errorFromCode(FailureCodes.SYSTEM_ADDED_CONFIG_EXISTS));
            return;
        }
        if (unbind) {
            bindProcessToNetwork(null);
        }

        callback.invoke();
    }


    private boolean removeSSID(String ssid) {
        boolean success = true;
        // Remove the existing configuration for this network
        WifiConfiguration existingNetworkConfigForSSID = getExistingNetworkConfig(ssid);

        //No Config found
        if (existingNetworkConfigForSSID == null) {
            return success;
        }
        int existingNetworkId = existingNetworkConfigForSSID.networkId;
        if (existingNetworkId == -1) {
            return success;
        }
        success = wifiManager.removeNetwork(existingNetworkId) && wifiManager.saveConfiguration();
        //If not our config then success would be false
        return success;
    }

    @ReactMethod
    public void getSSID(Callback callback) {
        String ssid = this.getWifiSSID();
        callback.invoke(ssid);
    }

    private String getWifiSSID() {
        WifiInfo info = wifiManager.getConnectionInfo();
        String ssid = info.getSSID();

        if (ssid == null || ssid.equalsIgnoreCase("<unknown ssid>")) {
            NetworkInfo nInfo = connectivityManager.getActiveNetworkInfo();
            if (nInfo != null && nInfo.isConnected()) {
                ssid = nInfo.getExtraInfo();
            }
        }

        if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }

        return ssid;
    }

    private WifiConfiguration getExistingNetworkConfig(String ssid) {
        WifiConfiguration existingNetworkConfigForSSID = null;
        List<WifiConfiguration> configList = wifiManager.getConfiguredNetworks();
        String comparableSSID = ('"' + ssid + '"'); // Add quotes because wifiConfig.SSID has them
        if (configList != null) {
            for (WifiConfiguration wifiConfig : configList) {
                if (wifiConfig.SSID.equals(comparableSSID)) {
                    Log.d("WifiTools", "Found Matching Wifi: "+ wifiConfig.toString());
                    existingNetworkConfigForSSID = wifiConfig;
                    break;

                }
            }
        }
        return existingNetworkConfigForSSID;
    }
}
