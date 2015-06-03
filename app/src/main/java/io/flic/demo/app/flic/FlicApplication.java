package io.flic.demo.app.flic;


import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

public class FlicApplication extends Application {

    private static FlicApplication app;
    public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private HashMap<String, FlicButton> buttons;
    private HashMap<String, FlicButtonUpdateListener> buttonListeners;
    private HashMap<String, HashMap<String, FlicButtonEventListener>> buttonEventListeners;
    private ServiceConnection serviceConnection;
    private FlicService flicService = null;

    public static FlicApplication getApp() {
        if (FlicApplication.app == null) {
            throw new RuntimeException("App hasn't been created yet");
        } else {
            return FlicApplication.app;
        }
    }

    public boolean isBluetoothActive() {
        return bluetoothAdapter.isEnabled();
    }

    @Override
    public void onCreate() {
        FlicApplication.app = this;

        this.buttons = new HashMap<>();
        this.buttonListeners = new HashMap<>();
        this.buttonEventListeners = new HashMap<>();

        Intent i = new Intent(this, FlicService.class);
        this.startService(i);

        this.serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                FlicService.FlicBinder binder = (FlicService.FlicBinder) service;
                FlicApplication.this.flicService = binder.getService();
                for (io.flic.lib.FlicButton flicButton : FlicApplication.this.flicService.getButtons()) {
                    boolean connected = flicButton.getConnectionState() == io.flic.lib.FlicButton.STATE_CONNECTED;
                    String deviceId = flicButton.getButtonId().toLowerCase();
                    FlicButton button = new FlicButton(
                            deviceId,
                            FlicButton.FlicColor.FLIC_COLOR_MINT,
                            connected);
                    FlicApplication.this.buttons.put(button.getDeviceId(), button);
                    for (FlicButtonUpdateListener listener : FlicApplication.this.buttonListeners.values()) {
                        listener.buttonAdded(button);
                    }
                    if (!connected) {
                        FlicApplication.this.flicService.connectButton(deviceId);
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                FlicApplication.this.flicService = null;
            }
        };

        this.bindService(i, this.serviceConnection, 0);
    }

    public void addButtonUpdateListener(FlicButtonUpdateListener listener) {
        this.buttonListeners.put(listener.getHash(), listener);
    }

    public void addButtonEventListener(String deviceId, FlicButtonEventListener listener) {
        HashMap<String, FlicButtonEventListener> listeners;
        if (this.buttonEventListeners.containsKey(deviceId)) {
            listeners = this.buttonEventListeners.get(deviceId);
        } else {
            listeners = new HashMap<>();
            this.buttonEventListeners.put(deviceId, listeners);
        }
        listeners.put(listener.getHash(), listener);
    }

    public void removeButtonUpdateListener(String hash) {
        this.buttonListeners.remove(hash);
    }

    public void removeButtonEventListener(String hash) {
        for (Entry<String, HashMap<String, FlicButtonEventListener>> entry : this.buttonEventListeners.entrySet()) {
            if (entry.getValue().containsKey(hash)) {
                entry.getValue().remove(hash);
            }
        }
    }

    public FlicButton getButton(String deviceId) {
        return this.buttons.get(deviceId);
    }

    public Collection<FlicButton> getButtons() {
        return this.buttons.values();
    }

    public void startScan() {
        this.flicService.startScan();
    }

    public void stopScan() {
        this.flicService.stopScan();
    }

    public void scanFor(int milliseconds) {
        this.flicService.scanFor(milliseconds);
    }

    public void connectButton(String deviceId) {
        if (!this.buttons.containsKey(deviceId)) {
            FlicButton flicButton = new FlicButton(deviceId, FlicButton.FlicColor.FLIC_COLOR_MINT, false);
            this.buttons.put(deviceId, flicButton);
            for (FlicButtonUpdateListener listener : this.buttonListeners.values()) {
                listener.buttonAdded(flicButton);
            }
        }
        this.flicService.connectButton(deviceId);
    }

    public void disconnectButton(String deviceId) {
        if (this.buttons.containsKey(deviceId)) {
            this.flicService.disconnectButton(deviceId);
        } else {
            throw new RuntimeException("Unknown button");
        }
    }

    public void deleteButton(String deviceId) {
        if (this.buttons.containsKey(deviceId)) {
            this.buttons.remove(deviceId);
            this.flicService.deleteButton(deviceId);
        }
    }

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String base64DeviceId(String deviceId) {
        String hex = deviceId.substring(9).replace(":", "");
        byte[] bytes = hexToBytes(hex);
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP);
    }

    public void notifyButtonDiscover(String deviceId, int rssi, boolean isPrivateMode) {
        for (FlicButtonUpdateListener listener : this.buttonListeners.values()) {
            listener.buttonDiscovered(deviceId, rssi, isPrivateMode);
        }
    }

    public void notifyButtonConnect(String deviceId, String uuid) {
        for (FlicButtonUpdateListener listener : this.buttonListeners.values()) {
            listener.buttonConnected(deviceId);
        }

        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonConnected(deviceId, uuid);
            }
        }
    }

    public void notifyButtonReady(String deviceId, String uuid) {
        FlicButton flicButton = this.buttons.get(deviceId);
        if (flicButton != null) {
            flicButton.setConnected();
            for (FlicButtonUpdateListener listener : this.buttonListeners.values()) {
                listener.buttonReady(deviceId);
            }

            if (this.buttonEventListeners.containsKey(deviceId)) {
                for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                    listener.buttonReady(deviceId, uuid);
                }
            }
        } else {
            Log.i("FlicApplication", "notifyButtonReady: unknown button");
        }
    }

    public void notifyButtonConnectionFailed(String deviceId, int status) {
        FlicButton flicButton = this.buttons.get(deviceId);
        if (flicButton != null) {
            flicButton.setDisconnected();
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonConnectionFailed(deviceId, status);
            }
        }
    }

    public void notifyButtonDisconnect(String deviceId, int status) {
        FlicButton flicButton = this.buttons.get(deviceId);
        if (flicButton != null) {
            flicButton.setDisconnected();
            for (FlicButtonUpdateListener listener : this.buttonListeners.values()) {
                listener.buttonDisconnected(deviceId, status);
            }
            if (this.buttonEventListeners.get(deviceId) != null) {
                for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                    listener.buttonDisconnected(deviceId, status);
                }
            }
        } else {
            Log.i("FlicApplication", "notifyButtonReady: unknown button");
        }
    }

    public void notifyButtonDown(String deviceId) {
        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonDown(deviceId);
            }
        }
    }

    public void notifyButtonUp(String deviceId) {
        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonUp(deviceId);
            }
        }
    }

    public void notifyButtonClick(String deviceId) {
        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonClick(deviceId);
            }
        }
    }

    public void notifyButtonDoubleClick(String deviceId) {
        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonDoubleClick(deviceId);
            }
        }
    }

    public void notifyButtonHold(String deviceId) {
        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonHold(deviceId);
            }
        }
    }

    private FlicService getFlicService() {
        return this.flicService;
    }
}
