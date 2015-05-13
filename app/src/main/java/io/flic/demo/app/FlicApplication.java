package io.flic.demo.app;


import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

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
        Log.i("FlicApplication", "onCreate");

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
                    FlicButton button = new FlicButton(
                            flicButton.getButtonId(),
                            flicButton.getButtonUuid(),
                            FlicButton.FlicColor.FLIC_COLOR_MINT,
                            connected);
                    for (FlicButtonUpdateListener listener : FlicApplication.this.buttonListeners.values()) {
                        listener.buttonAdded(button);
                    }
                    if (!connected) {
                        FlicApplication.this.flicService.connectButton(flicButton.getButtonId());
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

    public void saveButton(FlicButton flicButton) {
        this.buttons.put(flicButton.getDeviceId(), flicButton);
        for (FlicButtonUpdateListener listener : this.buttonListeners.values()) {
            listener.buttonAdded(flicButton);
        }
    }

    public FlicButton getButton(String deviceId) {
        return this.buttons.get(deviceId);
    }

    public Collection<FlicButton> getButtons() {
        return this.buttons.values();
    }

    public void notifyButtonDiscover(String deviceId, int rssi, boolean isPrivateMode) {

        if (this.buttons.containsKey(deviceId)) {
            FlicButton flicButton = this.buttons.get(deviceId);
            this.flicService.connectButton(deviceId);
        }
        for (FlicButtonUpdateListener listener : this.buttonListeners.values()) {
            listener.buttonDiscovered(deviceId, rssi, isPrivateMode);
        }
    }

    public void notifyButtonConnect(String deviceId, String uuid) {
        Log.i("FlicApplication", "notifyButtonConnect: " + deviceId);
        Log.i("FlicApplication.notifyButtonConnect", "ButtonUpdateListeners: " + this.buttonListeners.values().size());
        for (FlicButtonUpdateListener listener : this.buttonListeners.values()) {
            listener.buttonConnected(deviceId);
        }

        if (this.buttonEventListeners.containsKey(deviceId)) {
            Log.i("FlicApplication.notifyButtonConnect", "ButtonEventListeners: " + this.buttonEventListeners.get(deviceId).values().size());
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonConnected(deviceId, uuid);
            }
        }
    }

    public void notifyButtonReady(String deviceId, String uuid) {
        Log.i("FlicApplication.notifyButtonReady", "notifyButtonReady: " + deviceId);
        FlicButton flicButton = this.buttons.get(deviceId);
        if (flicButton != null) {
            Log.i("FlicApplication", "Setting connected true");
            flicButton.setConnected();
        }
        Log.i("FlicApplication", "ButtonUpdateListeners: " + this.buttonListeners.values().size());
        for (FlicButtonUpdateListener listener : this.buttonListeners.values()) {
            listener.buttonReady(deviceId);
        }

        if (this.buttonEventListeners.containsKey(deviceId)) {
            Log.i("FlicApplication", "ButtonEventListeners: " + this.buttonEventListeners.get(deviceId).values().size());
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonReady(deviceId, uuid);
            }
        }
    }

    public void notifyButtonConnectionFailed(String deviceId, int status) {
        Log.i("FlicApplication", "notifyButtonConnectionFailed: " + deviceId);

        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonConnectionFailed(deviceId, status);
            }
        }
    }

    public void notifyButtonDisconnect(String deviceId, int status) {
        Log.i("FlicApplication.notifyButtonDisconnect", deviceId);
        FlicButton flicButton = this.buttons.get(deviceId);
        if (flicButton != null) {
            flicButton.setDisconnected();
        }
        for (FlicButtonUpdateListener listener : this.buttonListeners.values()) {
            listener.buttonDisconnected(deviceId, status);
        }
        if (this.buttonEventListeners.get(deviceId) != null) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonDisconnected(deviceId, status);
            }
        }
    }

    public void notifyButtonDown(String deviceId) {
        Log.i("FlicApplication.notifyButtonDown", deviceId);
        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonDown(deviceId);
            }
        }

    }

    public void notifyButtonUp(String deviceId) {
        Log.i("FlicApplication.notifyButtonUp", deviceId);
        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonUp(deviceId);
            }
        }
    }

    public void notifyButtonClick(String deviceId) {
        Log.i("FlicApplication.notifyButtonClick", deviceId);
        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonClick(deviceId);
            }
        }
    }

    public void notifyButtonDoubleClick(String deviceId) {
        Log.i("FlicApplication.notifyButtonDoubleClick", deviceId);
        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonDoubleClick(deviceId);
            }
        }
    }

    public void notifyButtonHold(String deviceId) {
        Log.i("FlicApplication.notifyButtonHold", deviceId);
        if (this.buttonEventListeners.containsKey(deviceId)) {
            for (FlicButtonEventListener listener : this.buttonEventListeners.get(deviceId).values()) {
                listener.buttonHold(deviceId);
            }
        }
    }

    public FlicService getFlicService() {
        return this.flicService;
    }
}
