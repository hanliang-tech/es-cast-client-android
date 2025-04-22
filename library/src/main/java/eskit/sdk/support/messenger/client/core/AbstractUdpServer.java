package eskit.sdk.support.messenger.client.core;

import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.CountDownLatch;

import eskit.sdk.support.messenger.client.utils.NetUtils;

public abstract class AbstractUdpServer implements Runnable {

    private static final String TAG = "[-EsMessenger-]";

//    private static final String[] NET_FILTER = new String[]{"rmnet", "vmnet", "vnic", "vboxnet", "virtual", "ppp"};

    private DatagramChannel mChannel;
    private boolean mRunning;
    private ByteBuffer mBuffer;
    private int mPortSearchStart;
    private String mIp;
    private String mIpPrefix;

    private final CountDownLatch mServerStartLatch = new CountDownLatch(1);

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
            if (mChannel != null) {
                try {
                    mChannel.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        mChannel = null;
        mRunning = false;
    }

    public void send(String ip, int port, byte[] data) throws IOException, InterruptedException {
        if (mChannel == null) return;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        InetSocketAddress address = new InetSocketAddress(ip, port);
        while (mChannel.send(buffer, address) == 0){
            Log.d(TAG, "waiting buffer...");
            Thread.sleep(10);
        }
    }

    public String getLocalIp() {
        return mIp;
    }

    public String getLocalIpPrefix() {
        return mIpPrefix;
    }

    public int getPort() {
        return mChannel == null ? -1 : mChannel.socket().getLocalPort();
    }

    public int getSearchPort() {
        return mPortSearchStart;
    }

    public boolean isRunning() {
        return mRunning;
    }

    protected void onCreateUdpServer(UdpServerConfig config) {
        mBuffer = ByteBuffer.allocate(config.getBufferSize());
        mPortSearchStart = config.getPortSearchStart();
    }

    protected abstract void onReceiveData(DatagramPacket packet) throws Exception;

    protected abstract void onReceiveData(InetSocketAddress remote, byte[] data) throws Exception;

    @Override
    public void run() {
        try {
            mChannel = DatagramChannel.open();
//            mChannel.socket().setReuseAddress(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mChannel.bind(null);
            } else {
                mChannel.socket().bind(null);
            }
            mChannel.configureBlocking(false);

            int defaultSendBufferSize = mChannel.socket().getSendBufferSize();
            int defaultReceiveBufferSize = mChannel.socket().getReceiveBufferSize();
            // 设置发送缓冲区1024K
            int bufferSize = 1048576;
            mChannel.socket().setSendBufferSize(bufferSize);
            mChannel.socket().setReceiveBufferSize(bufferSize);

            Log.d(TAG, "send " + defaultSendBufferSize + " -> " + mChannel.socket().getSendBufferSize());
            Log.d(TAG, "receive " + defaultReceiveBufferSize + " -> " + mChannel.socket().getReceiveBufferSize());

            mRunning = true;
            Log.d(TAG, "start listen on port " + getPort());

            // 创建Selector 阻塞接收 降低CPU占用
            Selector selector = Selector.open();
            mChannel.register(selector, SelectionKey.OP_READ);

            mServerStartLatch.countDown();

            while (isRunning()) {
                Log.d(TAG, "blocking...");
                int count = selector.select();
                if (count == 0) continue;
                while (true) {
                    mBuffer.clear();
                    InetSocketAddress remote = (InetSocketAddress) mChannel.receive(mBuffer);
                    if (remote == null) break;
                    mBuffer.flip();
                    byte[] data = new byte[mBuffer.remaining()];
                    mBuffer.get(data);
                    onReceiveData(remote, data);
                }
                selector.selectedKeys().clear();
            }
        } catch (Exception e) {
            Log.w(TAG, "exit: " + e);
        }
    }

    protected String toString(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength());
    }

    public CountDownLatch getLock() {
        return mServerStartLatch;
    }
}