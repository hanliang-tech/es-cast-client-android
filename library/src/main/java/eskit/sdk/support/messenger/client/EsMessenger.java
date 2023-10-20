package eskit.sdk.support.messenger.client;

import android.content.Context;

import eskit.sdk.support.messenger.client.bean.EsDevice;
import eskit.sdk.support.messenger.client.core.EsCommand;
import eskit.sdk.support.messenger.client.core.UdpHandler;

/**
 *
 */
public class EsMessenger implements IEsMessenger {

    private IEsMessenger.MessengerCallback mDeviceCallback;
    private volatile UdpHandler mUdpHandler;

    private synchronized void initUdpServerIfNeed() {
        if (mUdpHandler == null) {
            mUdpHandler = new UdpHandler();
        }
    }

    @Override
    public void setMessengerCallback(MessengerCallback callback) {
        mDeviceCallback = callback;
    }

    @Override
    public void search(Context context) {
        stop();
        initUdpServerIfNeed();
        mUdpHandler.search(context);
    }

    @Override
    public void ping(Context context, EsDevice device) {
        initUdpServerIfNeed();
        mUdpHandler.ping(context, device);
    }

    @Override
    public void stop() {
        if (mUdpHandler != null) {
            mUdpHandler.safeStop();
        }
        mUdpHandler = null;
    }

    public void sendCommand(Context context, EsDevice device, EsCommand command) {
        initUdpServerIfNeed();
        mUdpHandler.sendCommandEvent(context, device, command);
    }

    public IEsMessenger.MessengerCallback getCallback() {
        return mDeviceCallback;
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
