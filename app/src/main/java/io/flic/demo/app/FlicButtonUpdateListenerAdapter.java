package io.flic.demo.app;

public abstract class FlicButtonUpdateListenerAdapter implements FlicButtonUpdateListener {

    @Override
    public void buttonDiscovered(String deviceId, int rssi, boolean isPrivateMode) {

    }

    @Override
    public void buttonConnected(String deviceId) {

    }

    @Override
    public void buttonReady(String deviceId) {

    }

    @Override
    public void buttonDisconnected(String deviceId, int status) {

    }

    @Override
    public void buttonAdded(FlicButton flicButton) {

    }

    @Override
    public void buttonDeleted(FlicButton flicButton) {

    }
}

