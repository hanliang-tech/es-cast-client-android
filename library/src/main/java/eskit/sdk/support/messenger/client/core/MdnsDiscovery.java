package eskit.sdk.support.messenger.client.core;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.util.Log;

import java.util.LinkedList;
import java.util.Map;

import eskit.sdk.support.messenger.client.bean.EsDevice;

public class MdnsDiscovery {

    private static final String TAG = "[-EsMessenger-]";
    private static final String SERVICE_TYPE = "_eskit._udp";

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private DeviceDiscoveryCallback mCallback;
    private volatile boolean mRunning;

    private final LinkedList<NsdServiceInfo> mResolveQueue = new LinkedList<>();
    private volatile boolean mResolving;

    public void start(Context context, DeviceDiscoveryCallback callback) {
        if (mRunning) return;
        mCallback = callback;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        if (mNsdManager == null) {
            Log.w(TAG, "mDNS: NsdManager not available");
            return;
        }

        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.w(TAG, "mDNS: start discovery failed, error=" + errorCode);
                mRunning = false;
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.w(TAG, "mDNS: stop discovery failed, error=" + errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "mDNS: discovery started");
                mRunning = true;
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "mDNS: discovery stopped");
                mRunning = false;
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "mDNS: service found: " + serviceInfo.getServiceName());
                enqueueResolve(serviceInfo);
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "mDNS: service lost: " + serviceInfo.getServiceName());
            }
        };

        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stop() {
        mRunning = false;
        mCallback = null;
        if (mNsdManager != null && mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } catch (Exception e) {
                Log.w(TAG, "mDNS: stop error: " + e);
            }
        }
        synchronized (mResolveQueue) {
            mResolveQueue.clear();
        }
        mDiscoveryListener = null;
        mNsdManager = null;
    }

    private void enqueueResolve(NsdServiceInfo serviceInfo) {
        synchronized (mResolveQueue) {
            mResolveQueue.add(serviceInfo);
            if (!mResolving) {
                drainResolveQueue();
            }
        }
    }

    private void drainResolveQueue() {
        NsdServiceInfo next;
        synchronized (mResolveQueue) {
            next = mResolveQueue.poll();
            if (next == null) {
                mResolving = false;
                return;
            }
            mResolving = true;
        }

        if (mNsdManager == null) return;

        mNsdManager.resolveService(next, new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.w(TAG, "mDNS: resolve failed: " + serviceInfo.getServiceName() + ", error=" + errorCode);
                drainResolveQueue();
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                if (!mRunning) return;
                try {
                    EsDevice device = toEsDevice(serviceInfo);
                    if (device == null) {
                        drainResolveQueue();
                        return;
                    }
                    DeviceDiscoveryCallback cb = mCallback;
                    if (cb != null) {
                        cb.onDeviceDiscovered(device);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "mDNS: convert device error: " + e);
                }
                drainResolveQueue();
            }
        });
    }

    private EsDevice toEsDevice(NsdServiceInfo serviceInfo) {
        if (serviceInfo.getHost() == null) {
            Log.w(TAG, "mDNS: resolved service has null host, skip");
            return null;
        }
        EsDevice device = new EsDevice();
        device.setDeviceIp(serviceInfo.getHost().getHostAddress());
        device.setDevicePort(serviceInfo.getPort());
        device.setDeviceName(serviceInfo.getServiceName());
        device.setDiscoveryType("mdns");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Map<String, byte[]> attributes = serviceInfo.getAttributes();
            if (attributes != null) {
                byte[] pkgBytes = attributes.get("pkg");
                if (pkgBytes != null) {
                    device.setFrom(new String(pkgBytes));
                }
            }
        }

        return device;
    }
}
