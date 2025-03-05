### 超级投屏-SDK-手机集成文档1.1.4
<br>
本sdk封装了发现扩展屏服务的相关接口。
<br>
<br>
<br>

| 版本 | 修改点 | 修改日期 |
|-|-|-|
| 1.1.1 | 支持新的设备类型发现 |2023-10-19|
| 1.1.2 | 增加设置`OAID/AAID`[接口](#jump1) |2023-11-06|
| 1.1.3 | EsCommand暴露`put`方法,详见[#4.1](#jump2)、[#4.2](#jump3) |2023-11-16|
| 1.1.4 | MessengerCallback新增[onPingResponse](#jump4)方法 |2025-03-05|

<br>
<br>

#### 1、集成

``` java
# 工程根gradle增加sdk的maven仓库
allprojects {
    repositories {
        maven { url 'https://nexus.extscreen.com/repository/maven-public/' }
    }
}

# app gradle增加引用
dependencies {
    implementation 'com.extscreen.sdk:messenger-client:1.1.4'
}

```

#### <span id="jump1">2、sdk调用</span>
``` java

// 注册sdk回调
EsMessenger.get().setMessengerCallback(MessengerCallback callback);

// 搜索设备
EsMessenger.get().search(context);

// 检测设备是否在线
EsMessenger.get().ping(Context context, EsDevice device);

// 发送命令
EsMessenger.get().sendCommand(Context context, EsDevice device, EsCommand command);

// 停止并释放资源
EsMessenger.get().stop();

// 设置OAID, 非必传
EsMessenger.get().setOAID("oaid");

// 设置AAID, 非必传
EsMessenger.get().setAAID("aaid");

```

<span id="jump4">事件回调接口</span>
``` java
    /**
     * 事件回调
     **/
    interface MessengerCallback {
    
        /**
         * Ping设备响应
         * @param deviceIp
         * @param devicePort
         */
        void onPingResponse(String deviceIp, int devicePort);
        
        /**
         * 发现设备
         **/
        void onFindDevice(EsDevice device);

        /**
         * 收到消息
         *
         * @param event
         **/
        void onReceiveEvent(EsEvent event);
    }
}
```

Device字段说明
``` java
// EsDevivce.java
public class EsDevice {
    private String deviceName;      // 发现设备的名称
    private String deviceIp;        // 发现设备的ip
    private int devicePort;         // 发现设备的端口
    private String from;            // 发现来源的apk包名
    private final long findTime;    // 发现时间
    private int version;            // 协议版本
}
```

#### 3、基本使用

``` kotlin
// 注册SDK回调
EsMessenger.get().setMessengerCallback(object : IEsMessenger.MessengerCallback {

     override fun onPingResponse(deviceIp: String, devicePort:Int) {
        handler.post{
            Toast.makeText(this, "设备 ${deviceIp}:${devicePort} 在线", Toast.LENGTH_LONG).show()
        }
    }
    override fun onFindDevice(device: EsDevice) {
        deviceList.add(device)
        // 按发现时间排序
        deviceList.sortBy { it.findTime }
    }
    override fun onReceiveEvent(event: EsEvent) {
        handler.post {
            Toast.makeText(this, event.data, Toast.LENGTH_LONG).show()
        }
    }
})

// 调用搜索
EsMessenger.get().search(this)

// 调用关闭
override onDestroy(){
    EsMessenger.get().stop()
}

```

#### 4、启动快应用
``` kotlin
EsMessenger.get().sendCommand(this,
selectDevice, // 要发送的目标设备
EsCommand.makeEsAppCommand("es.hello.world")) // 快应用包名
```

##### <span id="jump2">4.1、启动快应用并关闭之前的界面</span>
flags 传 1，表示clearTask，会关掉之前的界面，再打开新的。
``` kotlin
EsMessenger.get().sendCommand(this,
selectDevice, // 要发送的目标设备
EsCommand.makeEsAppCommand("es.hello.world"), // 快应用包名
.put("flags",1))
```

##### <span id="jump3">4.2、复用之前的界面</span>
`flags` 传 8，表示singleInstance，会复用的界面，不再打开新的。
但要配合`pageTag`使用，相同pageTag会复用同一界面。
``` kotlin
EsMessenger.get().sendCommand(this,
selectDevice, // 要发送的目标设备
EsCommand.makeEsAppCommand("es.hello.world"), // 快应用包名
.put("flags", 8)
.put("pageTag", "pageTag")) // pageTag可任意定义
```

#### 5、启动快应用指定页面并传递参数
``` kotlin
EsMessenger.get().sendCommand(this, 
selectDevice, // 要发送的目标设备
EsCommand.makeEsAppCommand("es.com.ergediandian.tv")  // 快应用包名
.apply {
    setEventData(
        CmdArgs("series_view") // 页面路径
            .put("mediaId", "1499655936638648322") // 参数一
            .put("episodeId", "1499774081069404161") // 参数二
    )
})
```

#### 6、关闭快应用
``` kotlin
EsMessenger.get().sendCommand(this, 
    selectDevice, 
    EsCommand.makeCmdCloseCommand("快应用包名")
)

```

#### 7、发送遥控器事件
由于权限问题，只能当应用在前台的时候才能生效
``` kotlin
EsMessenger.get().sendCommand(this, selectDevice, EsCommand.makeCmdKeyEventCommand(KeyEvent.KEYCODE_VOLUME_UP)) // 音量上键
```