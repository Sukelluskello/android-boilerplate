package io.flic.demo.app.flic;

public class FlicButton {

    private final String deviceId;
    private final FlicColor color;
    private boolean connected;

    public FlicButton(String deviceId, FlicColor color, boolean connected) {
        this.deviceId = deviceId;
        this.color = color;
        this.connected = connected;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public FlicColor getColor() {
        return FlicColor.FLIC_COLOR_MINT;
    }


    public void setConnected() {
        this.connected = true;
    }

    public void setDisconnected() {
        this.connected = false;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public static enum FlicColor {
        FLIC_COLOR_BLACK,
        FLIC_COLOR_WHITE,
        FLIC_COLOR_MINT,
        FLIC_COLOR_YELLOW
    }
}

