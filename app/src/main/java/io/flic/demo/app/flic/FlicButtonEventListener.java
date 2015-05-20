package io.flic.demo.app.flic;

public interface FlicButtonEventListener {
    public void buttonDown(String deviceId);

    public void buttonUp(String deviceId);

    public void buttonClick(String deviceId);

    public void buttonDoubleClick(String deviceId);

    public void buttonHold(String deviceId);

    public void buttonConnected(String deviceId, String UUID);

    public void buttonReady(String deviceId, String UUID);

    public void buttonConnectionFailed(String deviceId, int status);

    public void buttonDisconnected(String deviceId, int status);

    public String getHash();
}

