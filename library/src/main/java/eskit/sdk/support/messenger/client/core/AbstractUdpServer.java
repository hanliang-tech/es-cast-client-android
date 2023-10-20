package eskit.sdk.support.messenger.client.core;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import eskit.sdk.support.messenger.client.utils.NetUtils;

public abstract class AbstractUdpServer implements Runnable {

    private static final String TAG = "[-EsMessenger-]";

    private static final String[] NET_FILTER = new String[]{"rmnet", "vmnet", "vnic", "vboxnet", "virtual", "ppp"};

    private DatagramSocket mSocket;
    private boolean mRunning;
    private byte[] mBuffer;
    private int mPortSearchStart;
    private String mIp;
    private String mIpPrefix;

    public AbstractUdpServer() {
        try {
            mIp = NetUtils.getIp();
            if (mIp != null && mIp.contains(".")) {
                mIpPrefix = mIp.substring(0, mIp.lastIndexOf(".") + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        onCreateUdpServer(new UdpServerConfig());
        new Thread(this).start();
    }

    public void stop() {
        if (isRunning()) {
            if (mSocket != null) {
                try {
                    mSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        mSocket = null;
        mRunning = false;
    }

    public void send(DatagramPacket packet) throws IOException {
        if (mSocket == null) return;
        mSocket.send(packet);
    }

    public String getLocalIp() {
        return mIp;
    }

    public String getLocalIpPrefix() {
        return mIpPrefix;
    }

    public int getPort() {
        return mSocket == null ? -1 : mSocket.getLocalPort();
    }

    public int getSearchPort() {
        return mPortSearchStart;
    }

    public boolean isRunning() {
        return mRunning;
    }

    protected void onCreateUdpServer(UdpServerConfig config) {
        mBuffer = new byte[config.getBufferSize()];
        mPortSearchStart = config.getPortSearchStart();
    }

    protected abstract void onReceiveData(DatagramPacket packet) throws Exception;

    @Override
    public void run() {
        try {
            mSocket = new DatagramSocket();
            mSocket.setReuseAddress(true);
            mRunning = true;
            Log.d(TAG, "start listen on port " + getPort());
            while (isRunning()) {
                DatagramPacket packet = new DatagramPacket(mBuffer, mBuffer.length);
                mSocket.receive(packet);
                if (packet.getLength() <= 0) continue;
                onReceiveData(packet);
            }
        } catch (Exception e) {
            Log.w(TAG, "exit: " + e);
        }
    }

    protected String toString(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength());
    }

    protected JSONObject toJson(String data) {
        try {
            return new JSONObject(data);
        } catch (Exception ignored) {
        }
        return null;
    }
}