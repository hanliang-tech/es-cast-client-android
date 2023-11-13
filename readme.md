### 超级投屏-SDK-手机集成文档

<br>
本sdk封装了发现扩展屏服务的相关接口。
<br>
<br>
<br>

| 版本 | 修改点 |
|-|-|
| 1.1.1 | 支持新的设备类型发现 |
| 1.1.2 | 增加设置`OAID/AAID`接口 |

<br>
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
    implementation 'com.extscreen.sdk:messenger-client:1.1.2'
}

```

#### 2、sdk调用
``` java

// 设置OAID, 非必传
EsMessenger.get().setOAID("oaid");

// 设置AAID, 非必传
EsMessenger.get().setAAID(String AAID);

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
```

具体接口定义
``` java
    /**
     * 设置用户Android设备的OAID
     */
    void setOAID(String OAID);

    /**
    * 设置用户Android设备的AAID
    */
    void setAAID(String AAID);

    /**
     * 注册消息回调
     **/
    void setMessengerCallback(MessengerCallback callback);

    /**
     * 开始搜索
     **/
    void search(Context context);

    /**
     * Ping
     **/
    void ping(Context context, EsDevice device);

    /**
     * 发送命令
     **/
    void sendCommand(Context context, EsDevice device, EsCommand command);

    /**
     * 停止所有正在进行的任务，释放资源
     **/
    void stop();

    /**
     * 事件回调
     **/
    interface MessengerCallback {
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