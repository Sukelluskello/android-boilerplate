package io.flic.demo.app.flic;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;

import io.flic.demo.app.R;
import io.flic.demo.app.ui.MainActivity;
import io.flic.lib.FlicButton;
import io.flic.lib.FlicButtonAdapter;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerAdapter;

public class FlicService extends Service {

    private final static int SERVICE_NOTIFICATION_ID = 101;
    private final IBinder bind = new FlicBinder();
    private FlicManager manager;

    @Override
    public void onCreate() {
        Log.i("FlicService", "Starting FlicService");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Flic")
                .setTicker("Flic")
                .setContentText("Flic service")
                .setSmallIcon(R.drawable.flic_logo)
                .setContentIntent(contentIntent)
                .setOngoing(true).build();
        startForeground(SERVICE_NOTIFICATION_ID,
                notification);

        FlicManager.init(this.getApplicationContext());
        this.manager = FlicManager.getManager();
        this.manager.setMinimumSignalStrength(-100);
        this.manager.setEventListener(new FlicManagerAdapter() {
            @Override
            public boolean onDiscover(String deviceId, int rssi, boolean isPrivateMode) {
                deviceId = deviceId.toLowerCase();
                Log.i("FlicService", "onDiscover: " + deviceId);
                FlicService.this.buttonDiscovery(deviceId, rssi, isPrivateMode);
                return true;
            }
        });
        for (FlicButton flicButton : this.manager.getKnownButtons()) {
            Log.i("FlicService", "Starting pending connection to " + flicButton.getButtonId().toLowerCase());
            this.addFlicButtonListener(flicButton);
        }
    }

    private synchronized void buttonDiscovery(String deviceId, int rssi, boolean isPrivateMode) {
        this.getFlicApplication().notifyButtonDiscover(deviceId, rssi, isPrivateMode);
    }

    private void addFlicButtonListener(FlicButton flicButton) {
        flicButton.setEventListener(new FlicButtonAdapter() {
            @Override
            public void onConnect(FlicButton button) {
                String deviceId = button.getButtonId().toLowerCase();
                Log.i("FlicService", "onConnect: " + deviceId);
                FlicService.this.buttonConnect(deviceId, button.getButtonUuid());
            }

            @Override
            public void onReady(FlicButton button) {
                String deviceId = button.getButtonId().toLowerCase();
                Log.i("FlicService", "onReady: " + deviceId);
                FlicService.this.buttonReady(deviceId, button.getButtonUuid());
            }

            @Override
            public void onDisconnect(FlicButton button, int flicError) {
                String deviceId = button.getButtonId().toLowerCase();
                Log.i("FlicService", "onDisconnect: " + deviceId + " [error: " + flicError + "]");
                FlicService.this.buttonDisconnect(deviceId, flicError);
            }

            @Override
            public void onConnectionFailed(FlicButton button, int status) {
                String deviceId = button.getButtonId().toLowerCase();
                Log.i("FlicService", "onConnectionFailed: " + deviceId + " [status: " + status + "]");
                FlicService.this.buttonConnectionFailed(deviceId, status);
            }

            @Override
            public void onButtonUpOrDown(FlicButton button, boolean wasQueued, int timeDiff, boolean isUp, boolean isDown) {
                String deviceId = button.getButtonId().toLowerCase();
                if (isDown) {
                    Log.i("FlicService", "onButtonDown: " + deviceId);
                    FlicService.this.buttonDown(deviceId);
                } else {
                    Log.i("FlicService", "onButtonUp: " + deviceId);
                    FlicService.this.buttonUp(deviceId);
                }
            }

            @Override
            public void onButtonSingleOrDoubleClickOrHold(FlicButton button,
                                                          boolean wasQueued,
                                                          int timeDiff,
                                                          boolean isSingleClick,
                                                          boolean isDoubleClick,
                                                          boolean isHold) {
                String deviceId = button.getButtonId().toLowerCase();

                if (isSingleClick) {
                    Log.i("FlicService", "onButtonSingleClick: " + deviceId);
                    FlicService.this.buttonClick(deviceId);
                } else if (isDoubleClick) {
                    Log.i("FlicService", "onButtonDoubleClick: " + deviceId);
                    FlicService.this.buttonDoubleClick(deviceId);
                } else if (isHold) {
                    Log.i("FlicService", "onButtonHold: " + deviceId);
                    FlicService.this.buttonHold(deviceId);
                }
            }
        });
    }

    private FlicApplication getFlicApplication() {
        return (FlicApplication) this.getApplication();
    }

    private synchronized void buttonConnect(String deviceId, String uuid) {
        this.getFlicApplication().notifyButtonConnect(deviceId, uuid);
    }

    private synchronized void buttonReady(String deviceId, String uuid) {
        this.getFlicApplication().notifyButtonReady(deviceId, uuid);
    }

    private synchronized void buttonDisconnect(String deviceId, int status) {
        this.getFlicApplication().notifyButtonDisconnect(deviceId, status);
    }

    private synchronized void buttonConnectionFailed(String deviceId, int status) {
        this.getFlicApplication().notifyButtonConnectionFailed(deviceId, status);
    }

    private synchronized void buttonDown(String deviceId) {
        this.getFlicApplication().notifyButtonDown(deviceId);
    }

    private synchronized void buttonUp(String deviceId) {
        this.getFlicApplication().notifyButtonUp(deviceId);
    }

    private synchronized void buttonClick(String deviceId) {
        this.getFlicApplication().notifyButtonClick(deviceId);
    }

    private synchronized void buttonDoubleClick(String deviceId) {
        this.getFlicApplication().notifyButtonDoubleClick(deviceId);
    }

    private synchronized void buttonHold(String deviceId) {
        this.getFlicApplication().notifyButtonHold(deviceId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.bind;
    }

    public synchronized void startScan() {
        if (!this.manager.isScanning()) {
            Log.i("FlicService", "startScan");
            this.manager.startScan();
        } else {
            Log.i("FlicService", "startScan: already scanning");
        }
    }

    public synchronized void scanFor(final int milliseconds) {
        Log.i("FlicService", "scanFor " + milliseconds + "ms");
        if (!this.manager.isScanning()) {
            this.manager.startScan();
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        Thread.sleep(milliseconds);
                    } catch (InterruptedException e) {
                        Log.i("FlicService", "Scan timeout was interrupted", e);
                    }
                    if (FlicService.this.manager.isScanning()) {
                        FlicService.this.manager.stopScan();
                        Log.i("FlicService", "scanFor timeout: Stopped scan");
                    } else {
                        Log.i("FlicService", "scanFor timeout: Scan is not running");
                    }
                    return null;
                }
            }.execute();
        } else {
            Log.i("FlicService", "scanFor: already scanning");
        }
    }

    public synchronized void stopScan() {
        if (this.manager.isScanning()) {
            this.manager.stopScan();
            Log.i("FlicService", "Stopped scan");
        } else {
            Log.i("FlicService", "stopScan: Scan is not running");
        }
    }

    public synchronized void connectButton(String deviceId) {
        FlicButton button = this.manager.getButtonByDeviceId(deviceId);
        FlicService.this.addFlicButtonListener(button);
        button.connect();
        Log.i("FlicService", "Connecting to button: " + deviceId);
    }

    public synchronized void disconnectButton(String deviceId) {
        FlicButton button = this.manager.getButtonByDeviceId(deviceId);
        if (button != null) {
            button.disconnectOrAbortPendingConnection();
        } else {
            Log.i("FlicService", "disconnectButton: Cant find button: " + deviceId);
        }
    }

    public synchronized void deleteButton(String deviceId) {
        FlicButton button = this.manager.getButtonByDeviceId(deviceId);
        if (button != null) {
            this.manager.forgetButton(button);
            Log.i("FlicService", "Deleted Button: " + deviceId, new Exception());
        } else {
            Log.i("FlicService", "deleteButton: Cant find button: " + deviceId);
        }
    }

    public synchronized FlicButton getButton(String deviceId) {
        return this.manager.getButtonByDeviceId(deviceId);
    }

    public synchronized List<FlicButton> getButtons() {
        return this.manager.getKnownButtons();
    }

    public class FlicBinder extends Binder {
        public FlicService getService() {
            return FlicService.this;
        }
    }
}

