package eskit.sdk.support.messenger.client.bean;

/**
 *
 */
public class EsDevice {

    /** 设备名称 **/
    private String deviceName;
    /** 设备ip **/
    private String deviceIp;
    /** 设备端口 **/
    private int devicePort;
    /** 发现时间戳 **/
    private final long findTime;
    /** 来源 **/
    private String from;
    /** 协议版本 **/
    private int version;

    public EsDevice makeDevice(String ip, int port) {
        EsDevice device = new EsDevice();
        device.setDeviceIp(ip);
        device.setDevicePort(port);
        return device;
    }

    public EsDevice() {
        findTime = System.currentTimeMillis();
    }

    public String getDeviceName() {
        return deviceName;
    }

    public EsDevice setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public EsDevice setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
        return this;
    }

    public int getDevicePort() {
        return devicePort;
    }

    public EsDevice setDevicePort(int devicePort) {
        this.devicePort = devicePort;
        return this;
    }

    public long getFindTime() {
        return findTime;
    }

    public String getFrom() {
        return from;
    }

    public EsDevice setFrom(String from) {
        this.from = from;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public EsDevice setVersion(int version) {
        this.version = version;
        return this;
    }

    @Override
    public String toString() {
        return "EsDevice{" +
                "deviceName='" + deviceName + '\'' +
                ", deviceIp='" + deviceIp + '\'' +
                ", devicePort=" + devicePort +
                ", findTime=" + findTime +
                ", from='" + from + '\'' +
                ", version=" + version +
                '}';
    }
}
