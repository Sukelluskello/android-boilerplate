package io.flic.demo.app;

public class FlicButton {

    private final String deviceId;
    private final String uuid;
    private final FlicColor color;
    private boolean connected;

    public FlicButton(String deviceId, String uuid, FlicColor color, boolean connected) {
        this.deviceId = deviceId;
        this.uuid = uuid;
        this.color = color;
        this.connected = connected;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public String getUUID() {
        return this.uuid;
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

