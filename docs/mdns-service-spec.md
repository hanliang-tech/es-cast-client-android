# mDNS 服务规格说明（发现端参考）

## 概述

TV 端通过 Android `NsdManager` 在局域网注册 mDNS 服务，广播自身的 UDP 服务信息。手机端可通过 mDNS discovery 发现 TV 设备，获取 IP 和端口后直接发送 UDP 业务指令。

## 服务注册信息

| 字段 | 值 | 说明 |
|------|----|------|
| **Service Type** | `_eskit._udp` | DNS-SD 服务类型，`._udp` 表示底层传输协议 |
| **Service Name** | 动态，TV 端设置的设备名称 | 例如 "客厅电视"、"卧室电视"，同名时系统自动追加序号 |
| **Port** | 动态分配（默认从 5000 开始） | TV 端 UDP 服务的实际监听端口 |

## TXT Records（API 21+）

| Key | 值示例 | 说明 |
|-----|--------|------|
| `channel` | `general` / `tcl` / `ch` / `konka` | TV 端 SDK 渠道标识 |
| `pkg` | `com.extscreen.runtime` | TV 端目标 APK 包名 |

## 发现端实现指南

### 1. 发现服务

```java
NsdManager nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery start failed: " + errorCode);
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery stop failed: " + errorCode);
    }

    @Override
    public void onDiscoveryStarted(String serviceType) {
        Log.d(TAG, "Discovery started");
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        Log.d(TAG, "Discovery stopped");
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        // 发现服务后需要 resolve 才能拿到 IP 和 port
        nsdManager.resolveService(serviceInfo, resolveListener);
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "Service lost: " + serviceInfo.getServiceName());
    }
};

nsdManager.discoverServices("_eskit._udp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
```

### 2. 解析服务（获取 IP + Port）

```java
NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
    @Override
    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.e(TAG, "Resolve failed: " + errorCode);
    }

    @Override
    public void onServiceResolved(NsdServiceInfo serviceInfo) {
        String deviceName = serviceInfo.getServiceName();  // 设备名
        InetAddress host = serviceInfo.getHost();           // TV 的 IP
        int port = serviceInfo.getPort();                   // UDP 服务端口

        // TXT records (API 21+)
        Map<String, byte[]> attributes = serviceInfo.getAttributes();
        String channel = new String(attributes.get("channel"));  // general/tcl/ch/konka
        String pkg = new String(attributes.get("pkg"));          // 目标包名

        // 现在可以向 host:port 发送 UDP 指令
    }
};
```

### 3. 停止发现

```java
nsdManager.stopServiceDiscovery(discoveryListener);
```

## UDP 业务协议

发现设备后，手机端通过 UDP 向 `host:port` 发送 JSON 指令：

### 指令类型

| type | 名称 | 方向 | 说明 |
|------|------|------|------|
| `0` | PING | 手机 → TV → 手机 | 检测服务存活，TV 回复 `{"type":0}` |
| `1` | SEARCH | 手机 → TV → 手机 | 搜索设备信息，TV 回复 `{"type":1,"data":{"name":"设备名"}}` |
| `2` | EVENT | 手机 → TV | 发送投屏事件，`{"type":2,"data":"<json string>"}` |

### 示例：发送 PING

```java
JSONObject ping = new JSONObject();
ping.put("type", 0);
byte[] bytes = ping.toString().getBytes("UTF-8");
DatagramSocket socket = new DatagramSocket();
socket.send(new DatagramPacket(bytes, bytes.length, host, port));

// 接收回复
byte[] buf = new byte[1024];
DatagramPacket response = new DatagramPacket(buf, buf.length);
socket.receive(response);
String reply = new String(response.getData(), 0, response.getLength());
// reply = {"type":0}
```

## 注意事项

1. **`onServiceFound` 只返回 serviceName 和 serviceType**，必须调用 `resolveService()` 才能拿到 host 和 port
2. **NsdManager 并发 resolve 限制**：Android 系统同一时刻只允许一个 resolve 请求，多个设备需排队 resolve
3. **TXT records 在 API < 21 时不可用**，此时 `getAttributes()` 返回空 map，可通过 SEARCH 指令获取设备信息作为降级方案
4. **与 UDP 广播搜索的关系**：mDNS 是新增的发现方式，原有的 UDP 广播搜索仍然有效，两种方式可并存
5. **权限**：发现端可能需要 `INTERNET`、`ACCESS_WIFI_STATE`；Android 13+ 如果使用 Wi-Fi 感知功能可能需要 `NEARBY_WIFI_DEVICES`
