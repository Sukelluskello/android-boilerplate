package io.flic.demo.app;

public abstract class FlicButtonEventListenerAdapter implements FlicButtonEventListener {

    @Override
    public void buttonDown(String deviceId) {

    }

    @Override
    public void buttonUp(String deviceId) {

    }

    @Override
    public void buttonClick(String deviceId) {

    }

    @Override
    public void buttonDoubleClick(String deviceId) {

    }

    @Override
    public void buttonHold(String deviceId) {

    }

    @Override
    public void buttonConnected(String deviceId, String UUID) {

    }

    @Override
    public void buttonReady(String deviceId, String UUID) {

    }

    @Override
    public void buttonConnectionFailed(String deviceId, int status) {

    }

    @Override
    public void buttonDisconnected(String deviceId, int status) {

    }
}

