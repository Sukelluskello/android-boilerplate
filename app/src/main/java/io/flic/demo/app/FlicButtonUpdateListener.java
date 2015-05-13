package io.flic.demo.app;

public interface FlicButtonUpdateListener {
    public void buttonDiscovered(String deviceId, int rssi, boolean isPrivateMode);

    public void buttonConnected(String deviceId);

    public void buttonReady(String deviceId);

    public void buttonDisconnected(String deviceId, int status);

    public void buttonAdded(FlicButton flicButton);

    public void buttonDeleted(FlicButton flicButton);

    public String getHash();
}

