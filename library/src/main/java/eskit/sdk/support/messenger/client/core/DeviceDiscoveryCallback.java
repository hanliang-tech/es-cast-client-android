package eskit.sdk.support.messenger.client.core;

import eskit.sdk.support.messenger.client.bean.EsDevice;

public interface DeviceDiscoveryCallback {
    void onDeviceDiscovered(EsDevice device);
}
