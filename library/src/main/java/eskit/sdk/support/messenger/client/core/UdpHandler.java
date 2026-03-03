package eskit.sdk.support.messenger.client.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;

import eskit.sdk.support.messenger.client.BuildConfig;
import eskit.sdk.support.messenger.client.Configs;
import eskit.sdk.support.messenger.client.EsMessenger;
import eskit.sdk.support.messenger.client.IEsMessenger;
import eskit.sdk.support.messenger.client.bean.EsDevice;
import eskit.sdk.support.messenger.client.bean.EsEvent;

/**
 *
 */
public class UdpHandler extends BaseHandlerThread implements UdpCallback {

    private static final String TAG = "[-EsMessenger-]";

    private static final int CMD_PING = 0;
    private static final int CMD_SEARCH = 1;
    private static final int CMD_EVENT = 2;
    private static final int CMD_CUSTOM = 3;
    private static final int DEFAULT_SEARCH_PORT1 = 5000;
    private static final int DEFAULT_SEARCH_PORT2 = 5001;
    private static final int SEARCH_HOST_START = 1;
    private static final int SEARCH_HOST_END = 254;
    private static final int SEARCH_BROADCAST_SUFFIX = 255;
    private static final int SEARCH_BATCH_SIZE = 24;
    private static final int SEARCH_BATCH_SLEEP_MS = 2;
    private static final int SEARCH_ROUND_SLEEP_MS = 280;

    private UdpImpl mUdp;
    private JSONObject mEventBody;
    private String mCurrentServerIp;
    private int mCurrentServerPort;
    private final Object mSearchCacheLock = new Object();
    private final HashSet<String> mSearchDeviceCache = new HashSet<>();

    public UdpHandler(Context context) {
        super("udp-client");
        mUdp = new UdpImpl();
        start();
        mUdp.setCallback(this);
        mUdp.start(context);
    }

    public void search(Context context) {
        postWork(() -> {
            if (mUdp == null) return;
            clearSearchCache();
            try {
                mUdp.getLock().await();
            } catch (Exception ignore) {
            }
            try {
                startProxy(null);
                JSONObject jo = new JSONObject();
                jo.put("type", CMD_SEARCH);
                jo.put("device", getDeviceInfo(context));
                byte[] bytes = jo.toString().getBytes("UTF-8");
                int[] ports = getSearchPorts();
                String ipPrefix = mUdp.getLocalIpPrefix();
                if (TextUtils.isEmpty(ipPrefix)) {
                    Log.w(TAG, "search skip, local ip prefix empty");
                    return;
                }
                String localIp = mUdp.getLocalIp();
                Log.d(TAG, "search start");

                roundSend(() -> {
                    sendDataToSubnet(bytes, ipPrefix, localIp, ports);
                });

                Log.d(TAG, "search end");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void roundSend(Runnable runnable) {
        int round = Math.max(1, Configs.searchRound);
        for (int i = 0; i < round; i++) {
            runnable.run();
            if (i < round - 1) {
                sleepQuietly(SEARCH_ROUND_SLEEP_MS);
            }
        }
    }

    private int[] getSearchPorts() {
        int[] sourcePorts = (Configs.ports != null && Configs.ports.length > 0)
                ? Configs.ports : new int[]{DEFAULT_SEARCH_PORT1, DEFAULT_SEARCH_PORT2};
        HashSet<Integer> validPorts = new HashSet<>();
        for (int port : sourcePorts) {
            if (port > 0 && port <= 65535) {
                validPorts.add(port);
            }
        }
        if (validPorts.isEmpty()) {
            validPorts.add(DEFAULT_SEARCH_PORT1);
            validPorts.add(DEFAULT_SEARCH_PORT2);
        }
        int[] result = new int[validPorts.size()];
        int index = 0;
        for (Integer port : validPorts) {
            result[index++] = port;
        }
        return result;
    }

    private void sendDataToSubnet(byte[] data, String ipPrefix, String localIp, int[] ports) {
        int count = 0;
        for (int port : ports) {
            // 先发一次广播包，兼容只响应广播的设备实现
            sendData(data, ipPrefix + SEARCH_BROADCAST_SUFFIX, port);
            for (int i = SEARCH_HOST_START; i <= SEARCH_HOST_END; i++) {
                String ip = ipPrefix + i;
                if (TextUtils.equals(localIp, ip)) continue;
                sendData(data, ip, port);
                count++;
                if (count % SEARCH_BATCH_SIZE == 0) {
                    sleepQuietly(SEARCH_BATCH_SLEEP_MS);
                }
            }
        }
    }

    private void sleepQuietly(long sleep) {
        if (sleep <= 0) return;
        try {
            Thread.sleep(sleep);
        } catch (Exception ignore) {
        }
    }

    public void ping(Context context, EsDevice device) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", CMD_PING);
            data.put("device", getDeviceInfo(context));

            sendEvent(context, device, data, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendCommandEvent(Context context, EsDevice device, EsCommand command) {
        if (device == null) return;
        if (command.isStartEsApp()) {
            startProxy(device.getDeviceIp());
        }
        JSONObject data = new JSONObject();
        try {
            int type = (command.isCustom() && device.getVersion() > 0) ? CMD_CUSTOM : CMD_EVENT;
            data.put("type", type);
            data.put("device", getDeviceInfo(context));
            JSONObject args = command.getJsonObject();
            args.put("from", context.getPackageName());
            data.put("data", args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendEvent(context, device, data, command.isDebug());
    }

    private void sendEvent(Context context, EsDevice device, JSONObject data, boolean isDebug) {
        try {
            String json = data.toString();
            String ip = device.getDeviceIp();
            int port = device.getDevicePort();
            if (isDebug) {
                Log.i(TAG, " send to " + ip + ":" + port + " -> " + json);
            }
            byte[] bytes = json.getBytes("UTF-8");
            sendData(bytes, ip, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject getEventBody(Context context) {
        if (mEventBody == null) {
            JSONObject jo = new JSONObject();
            try {
                jo.put("device", getDeviceInfo(context));
            } catch (Exception e) {
                e.printStackTrace();
            }
            mEventBody = jo;
        }
        return mEventBody;
    }

    private void sendData(byte[] data, String ip, int port) {
        sendData(data, ip, port, 0);
    }

    private void sendData(byte[] data, String ip, int port, int sleep) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            postWork(() -> sendData(data, ip, port, sleep));
            return;
        }
        if (mUdp == null) return;
        try {
//                mUdp.send(new DatagramPacket(data, data.length, InetAddress.getByName(ip), port));
            mUdp.send(ip, port, data);
        } catch (Exception e) {
            Log.w(TAG, "send:" + e);
        }
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (Exception ignore) {
            }
        }
    }

    private void startProxy(String targetIp) {
        Log.i(TAG, "start proxy");
        byte[] flag = BuildConfig.UDP_PROXY_HEAD1;
        byte[] ip = "192.168.0.255".getBytes(Charset.forName("UTF-8"));
        JSONObject jo = new JSONObject();
        try {
            jo.putOpt("tclVersion", "1.0.0");
            jo.putOpt("deviceName", Build.DEVICE);
            jo.putOpt("wlanIp", mUdp.getLocalIpPrefix() + 1);
            jo.putOpt("type", 1);
            jo.putOpt("serviceData", "");
            jo.putOpt("extendServiceData", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] info = ("device_discover:" + jo).getBytes(Charset.forName("UTF-8"));

        ByteBuffer buffer = ByteBuffer.allocate(flag.length + ip.length + info.length + 2)
                .put(flag).put((byte) 0x23).put(ip).put((byte) 0x23).put(info);
        buffer.flip();
        byte[] data = buffer.array();

        if (!TextUtils.isEmpty(targetIp)) {
            sendData(data, targetIp, BuildConfig.UDP_PROXY_PORT);
        } else {
            for (int i = 2; i < 254; i++) {
                sendData(data, mUdp.getLocalIpPrefix() + i, BuildConfig.UDP_PROXY_PORT);
            }
        }
    }

    private void stopProxy() {
        if (!TextUtils.isEmpty(mCurrentServerIp)) {
            Log.i(TAG, "stop proxy");
            byte[] flag = BuildConfig.UDP_PROXY_HEAD2;
            sendData(flag, mCurrentServerIp, BuildConfig.UDP_PROXY_PORT);
            mCurrentServerIp = null;
        }
    }

    public void safeStop() {
        stopProxy();
        mUdp.stop();
        mUdp.setCallback(null);
        mUdp = null;
        mEventBody = null;
        clearSearchCache();
        quit();
    }

    private JSONObject getDeviceInfo(Context context) {
        JSONObject jo = new JSONObject();
        try {
            jo.put("brand", Build.BRAND);
            jo.put("model", Build.MODEL);
            jo.put("aos", Build.VERSION.SDK_INT);
            jo.put("pkg", context.getPackageName());
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            jo.put("verc", pi.versionCode);
            jo.put("vern", pi.versionName);
            jo.put("oaid", !TextUtils.isEmpty(Configs.oaid) ? Configs.oaid : "");
            jo.put("aaid", !TextUtils.isEmpty(Configs.aaid) ? Configs.aaid : "");
        } catch (Exception ignore) {
        }
        return jo;
    }

    @Override
    public void onReceiveUdpData(String ip, int port, String json) {
        IEsMessenger.MessengerCallback callback = EsMessenger.get().getCallback();
        if (callback == null) return;
        if (TextUtils.isEmpty(json)) return;
        try {
            JSONObject jo = new JSONObject(json);
            int eventType = jo.optInt("type");
            switch (eventType) {
                case CMD_PING: {
                    callback.onPingResponse(ip, port);
                }
                break;
                case CMD_SEARCH: {
                    JSONObject data = jo.optJSONObject("data");
                    if (data == null) {
                        data = jo.optJSONObject("device");
                    }
                    EsDevice device = new EsDevice();
                    device.setVersion(jo.optInt("version"));
                    device.setDeviceIp(ip);
                    device.setDevicePort(port);
                    String deviceName = jo.optString("name");
                    String from = jo.optString("pkg");
                    if (data != null) {
                        if (TextUtils.isEmpty(deviceName)) {
                            deviceName = data.optString("name");
                        }
                        if (TextUtils.isEmpty(deviceName)) {
                            deviceName = data.optString("deviceName");
                        }
                        if (TextUtils.isEmpty(from)) {
                            from = data.optString("pkg");
                        }
                        if (TextUtils.isEmpty(from)) {
                            from = data.optString("from");
                        }
                    }
                    if (TextUtils.isEmpty(deviceName)) {
                        deviceName = ip;
                    }
                    device.setDeviceName(deviceName);
                    device.setFrom(from);
                    if (!tryMarkSearchDevice(ip, port)) return;
                    callback.onFindDevice(device);
                }
                break;
                case CMD_EVENT: {
                    String jsonStr = jo.optString("data");
                    if (isProcessUpdateMessage(jsonStr)) {
                        mCurrentServerIp = ip;
                        mCurrentServerPort = port;
                        return;
                    }
                    EsEvent event = new EsEvent();
                    event.setDeviceIp(ip);
                    event.setDevicePort(port);
                    event.setData(jsonStr);
                    callback.onReceiveEvent(event);
                }
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isProcessUpdateMessage(String json) {
        try {
            JSONObject data = new JSONObject(json);
            String action = data.optString("action");
            if ("update".equals(action)) {
                return true;
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    private boolean tryMarkSearchDevice(String ip, int port) {
        synchronized (mSearchCacheLock) {
            return mSearchDeviceCache.add(ip + ":" + port);
        }
    }

    private void clearSearchCache() {
        synchronized (mSearchCacheLock) {
            mSearchDeviceCache.clear();
        }
    }

    private static final class UdpImpl extends AbstractUdpServer {

        private UdpCallback callback;

        public void setCallback(UdpCallback callback) {
            this.callback = callback;
        }

        @Override
        protected void onCreateUdpServer(UdpServerConfig config) {
            super.onCreateUdpServer(config);
        }

        @Override
        protected void onReceiveData(DatagramPacket packet) throws Exception {
            if (callback == null) return;
            try {
                callback.onReceiveUdpData(packet.getAddress().getHostAddress(), packet.getPort(), toString(packet));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onReceiveData(InetSocketAddress remote, byte[] data) throws Exception {
            if (callback == null) return;
            try {
                callback.onReceiveUdpData(remote.getAddress().getHostAddress(), remote.getPort(), new String(data));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
