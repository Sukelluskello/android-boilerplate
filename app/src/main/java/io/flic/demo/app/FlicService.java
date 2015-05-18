package io.flic.demo.app;

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
                Log.i("FlicService", "onConnect");
                FlicService.this.buttonConnect(button.getButtonId().toLowerCase(), button.getButtonUuid());
            }

            @Override
            public void onReady(FlicButton button) {
                Log.i("FlicService", "onReady");
                FlicService.this.buttonReady(button.getButtonId().toLowerCase(), button.getButtonUuid());
            }

            @Override
            public void onDisconnect(FlicButton button, int flicError) {
                Log.i("FlicService", "onDisconnect" + flicError);
                FlicService.this.buttonDisconnect(button.getButtonId().toLowerCase(), flicError);
            }

            @Override
            public void onConnectionFailed(FlicButton button, int status) {
                Log.i("FlicService", "onConnectionFailed");
                FlicService.this.buttonConnectionFailed(button.getButtonId().toLowerCase(), status);
            }

            @Override
            public void onButtonUpOrDown(FlicButton button, boolean wasQueued, int timeDiff, boolean isUp, boolean isDown) {
                if (isDown) {
                    FlicService.this.buttonDown(button.getButtonId().toLowerCase());
                } else {
                    FlicService.this.buttonUp(button.getButtonId().toLowerCase());
                }
            }

            @Override
            public void onButtonSingleOrDoubleClickOrHold(FlicButton button,
                                                          boolean wasQueued,
                                                          int timeDiff,
                                                          boolean isSingleClick,
                                                          boolean isDoubleClick,
                                                          boolean isHold) {
                Log.i("FlicService", "onButtonSingleOrDoubleClickOrHold");
                if (isSingleClick) {
                    FlicService.this.buttonClick(button.getButtonId().toLowerCase());
                } else if (isDoubleClick) {
                    FlicService.this.buttonDoubleClick(button.getButtonId().toLowerCase());
                } else if (isHold) {
                    FlicService.this.buttonHold(button.getButtonId().toLowerCase());
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
        Log.i("FlicService", "startScan", new Exception());
        if (!this.manager.isScanning()) {
            this.manager.startScan();
        }
    }

    public synchronized void scanFor(final int milliseconds) {
        if (!this.manager.isScanning()) {
            this.manager.startScan();
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        Thread.sleep(milliseconds);
                    } catch (InterruptedException ignored) {

                    }
                    if (FlicService.this.manager.isScanning()) {
                        FlicService.this.manager.stopScan();
                    }
                    return null;
                }
            }.execute();
        }
    }

    public synchronized void stopScan() {
        if (this.manager.isScanning()) {
            this.manager.stopScan();
        }
    }

    public synchronized void connectButton(String deviceId) {
        FlicButton button = this.manager.getButtonByDeviceId(deviceId);
        FlicService.this.addFlicButtonListener(button);
        button.connect();
    }

    public synchronized void disconnectButton(String deviceId) {
        FlicButton button = this.manager.getButtonByDeviceId(deviceId);
        if (button != null) {
            button.disconnectOrAbortPendingConnection();
        } else {
            Log.i("FlicService.disconnectButton", "Cant find button: " + deviceId);
        }
    }

    public synchronized void deleteButton(String deviceId) {
        FlicButton button = this.manager.getButtonByDeviceId(deviceId);
        if (button != null) {
            this.manager.forgetButton(button);
        } else {
            Log.i("FlicService.removeButton", "Cant find button: " + deviceId);
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

