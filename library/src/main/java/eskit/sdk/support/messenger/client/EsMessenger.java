package eskit.sdk.support.messenger.client;

import android.content.Context;

import java.util.HashSet;

import eskit.sdk.support.messenger.client.bean.EsDevice;
import eskit.sdk.support.messenger.client.core.EsCommand;
import eskit.sdk.support.messenger.client.core.MdnsDiscovery;
import eskit.sdk.support.messenger.client.core.UdpHandler;

/**
 *
 */
public class EsMessenger implements IEsMessenger {

    private IEsMessenger.MessengerCallback mDeviceCallback;
    private volatile UdpHandler mUdpHandler;
    private MdnsDiscovery mMdnsDiscovery;
    private final Object mDeduplicationLock = new Object();
    private final HashSet<String> mDiscoveredDevices = new HashSet<>();

    private synchronized void initUdpServerIfNeed(Context context) {
        if (mUdpHandler == null) {
            mUdpHandler = new UdpHandler(context);
        }
    }

    @Override
    public void setMessengerCallback(MessengerCallback callback) {
        mDeviceCallback = callback;
    }

    @Override
    public void search(Context context) {
        stop();
        initUdpServerIfNeed(context);
        mUdpHandler.search(context);
        mMdnsDiscovery = new MdnsDiscovery();
        mMdnsDiscovery.start(context, device -> {
            if (dedup(device)) {
                MessengerCallback cb = mDeviceCallback;
                if (cb != null) cb.onFindDevice(device);
            }
        });
    }

    @Override
    public void ping(Context context, EsDevice device) {
        initUdpServerIfNeed(context);
        mUdpHandler.ping(context, device);
    }

    @Override
    public void stop() {
        if (mUdpHandler != null) {
            mUdpHandler.safeStop();
        }
        mUdpHandler = null;
        if (mMdnsDiscovery != null) {
            mMdnsDiscovery.stop();
        }
        mMdnsDiscovery = null;
        clearDedup();
    }

    @Override
    public void setOAID(String OAID) {
        Configs.oaid = OAID;
    }

    @Override
    public void setAAID(String AAID) {
        Configs.aaid = AAID;
    }

    @Override
    public void setSearchRound(int round) {
        Configs.searchRound = Math.max(1, round);
    }

    @Override
    public void setSearchPorts(int[] ports) {
        Configs.ports = ports;
    }

    public void sendCommand(Context context, EsDevice device, EsCommand command) {
        initUdpServerIfNeed(context);
        mUdpHandler.sendCommandEvent(context, device, command);
    }

    public IEsMessenger.MessengerCallback getCallback() {
        return mDeviceCallback;
    }

    private boolean dedup(EsDevice device) {
        synchronized (mDeduplicationLock) {
            return mDiscoveredDevices.add(device.getDeviceIp() + ":" + device.getDevicePort());
        }
    }

    private void clearDedup() {
        synchronized (mDeduplicationLock) {
            mDiscoveredDevices.clear();
        }
    }

    //region 单例

    private static final class EsMessengerHolder {
        private static final EsMessenger INSTANCE = new EsMessenger();
    }

    public static EsMessenger get() {
        return EsMessengerHolder.INSTANCE;
    }

    private EsMessenger() {
    }

    //endregion

}
