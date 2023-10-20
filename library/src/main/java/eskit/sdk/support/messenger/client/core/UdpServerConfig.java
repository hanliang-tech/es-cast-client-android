package eskit.sdk.support.messenger.client.core;

public class UdpServerConfig {

    private int mPortSearchStart = 5000;
    private int mBufferSize = 1024;

    public int getPortSearchStart() {
        return mPortSearchStart;
    }

    public UdpServerConfig setPortSearchStart(int port) {
        this.mPortSearchStart = port;
        return this;
    }

    public UdpServerConfig setBufferSize(int size) {
        this.mBufferSize = size;
        return this;
    }

    public int getBufferSize() {
        return mBufferSize;
    }
}