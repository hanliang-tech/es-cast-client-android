package eskit.sdk.support.messenger.client.bean;

/**
 *
 */
public class EsEvent {
    private String deviceIp;
    private int devicePort;
    private String data;

    public String getDeviceIp() {
        return deviceIp;
    }

    public EsEvent setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
        return this;
    }

    public int getDevicePort() {
        return devicePort;
    }

    public EsEvent setDevicePort(int devicePort) {
        this.devicePort = devicePort;
        return this;
    }

    public String getData() {
        return data;
    }

    public EsEvent setData(String data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return "EsEvent{" +
                "deviceIp='" + deviceIp + '\'' +
                ", devicePort=" + devicePort +
                ", data='" + data + '\'' +
                '}';
    }
}
