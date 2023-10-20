package eskit.sdk.support.messenger.client.core;

public interface UdpCallback {
    void onReceiveUdpData(String ip, int port, String json);
}