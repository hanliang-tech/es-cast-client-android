## 超级投屏-SDK-手机集成文档1.1.4
<br>  

为方便开发者使用超级投屏协议，本SDK对协议功能进行了一层封装，可以快速实现:`发现设备`，`发送消息`，`消息接收`的功能。

[source-android](https://github.com/hanliang-tech/es-cast-client-android)：sdk android源码  
[source-ios](https://github.com/hanliang-tech/es-cast-client-ios)：sdk ios源码  
[source-vue](https://github.com/hanliang-tech/es-cast-vue-demo): 超级投屏demo vue源码

<br>

### 1、集成

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

### 2. 初始化
SDK初始化只需要一些简单的参数，但是是可选的，只为将来可能的业务场景预留，可不写。
``` java
// 设置可选扩展参数
EsMessenger.get().setOAID("oaid"); // 可选
EsMessenger.get().setAAID("aaid"); // 可选

// 注册SDK回调
EsMessenger.get().setMessengerCallback(MessengerCallback);
```

### 3. EsMessenger

EsMessenger提供了全部的API接口，注册回调、搜索、发送命令等。
**方法说明:**

| 方法 | 说明 |
|-|-|
|setMessengerCallback(MessengerCallback)| 注册SDK回调
|search(Context)| 搜索设备 [@搜索设备](#j_search)
|stop(Context)| 停止搜索
|ping(Context,EsDevice)| 检测设备是否在线
|sendCommand(Context,EsDevice,EsCommand)| 发送事件。所有的交互都是通过发送事件实现，详见[@发送事件](#j_send_evt)
|setOAID(String)| 设置OAID`可选`
|setAAID(String)| 设置AAID`可选`

### <span id="j_search">4. 搜索设备</span>
调用搜索设备之前，请确认已经注册了callback，当发现设备的时候，会接收到onFindDevice的回调方法。
``` java
EsMessenger.get().search(context);

// MessengerCallback回调
public void onFindDevice(EsDevice device) {
    // add to device list
}
```

**EsDevice说明:**

| 方法 | 说明 |
|-|-|
|getDeviceName| 设备名称
|getDeviceIp| 设备IP
|getDevicePort| 设备端口
|getFindTime| 发现设备的时间(毫秒), 特殊情况下可用于列表排序
|getFrom| 设备来源
|getVersion| 设备协议版本，暂未用到
|makeDevice| 创建一个远端设备。用于非搜索情况下知道的设备信息，来创建设备用于命令的发送。 `1.1.5新增`，对应底座SDK版本`!!!!!!!!!`

### 5. 检测设备是否在线
``` java
EsMessenger.get().ping(context, checkDevice);

// MessengerCallback回调
public void onPingResponse(String deviceIp, int devicePort) {
    if(Objects.equals(checkDevice.getDeviceIp(), deviceIp) &&
         checkDevice.getDevicePort() == devicePort) {
         // checkDevice online
    }
}
```

### <span id="j_send_evt">6. 发送/接收事件</span>
SDK将事件封装为EsCommand的类型，开发者只需要调用make方法生成对应类型的命令即可。
``` java
// 示例
// 初始化一个启动快应用的命令
EsCommand cmd = new EsCommand(ACTION_START_ES_APP).setPkgName(pkg);
EsMessenger.get().sendCommand(context, device, cmd);

// MessengerCallback回调
public void onReceiveEvent(EsEvent event) {
}
```
**EsCommand说明:**

| 方法 | 说明 |
|-|-|
|makeEsAppCommand(String)| 创建启动快应用的命令，传入快应用包名[@启动快应用](#j_es_app)
|makeNativeAppCommand(String)| 创建启动原生安卓应用的命令，传入原生应用包名
|makeCmdCloseCommand(String...)| 创建关闭快应用的命令，可以传多个包名。如需要关闭所有快应用，可传`all`
|makeCmdKeyEventCommand(int)|创建遥控器按键命令，仅当应用在前台时有效。[@遥控器事件](#j_remote_control)
|makeCustomCommand|创建跟大屏端自定义的私有命令
|setEventData|设置快应用启动的时候接收的参数
|put|可以设置启动快应用的参数，例如复用界面、隐藏启动页面等
|flagClearTask|启动快应用时关闭之前所有打开的快应用界面
|splashNone| 不展示loading界面
|setDebug| 是否打印发送的命令内容，调试的时候可以打开

#### <span id="j_es_app">6.1 启动快应用</span>
``` kotlin
// 启动包名为es.cast.demo的快应用

// A.每次发送都会拉起新的界面，按返回会逐级返回多个
val cmd = EsCommand.makeEsAppCommand("es.cast.demo")

// B.启动的时候关闭之前的，相当于clearTask
val cmd = EsCommand.makeEsAppCommand("es.cast.demo").put("flags", 1)
// 等同于
val cmd = EsCommand.makeEsAppCommand("es.cast.demo").flagClearTask()

// C.启动的时候会比对pageTag，相同的则会复用界面
// 例如多次打开播放页，只替换播放地址，不打开新的界面
val cmd = EsCommand.makeEsAppCommand("es.cast.demo")
.put("flags", 8)
.put("pageTag", "字符串类型，例如player")

// D.启动快应用并传递参数
val cmd = EsCommand.makeEsAppCommand("es.cast.demo")  // 快应用包名
.setEventData(
    CmdArgs("player") // 页面路径
        .put("mediaId", "1499655936638648322")
        .put("url", "https://hub.quicktvui.com/repository/public-files/video/dev/mp4/2.0/mp4-2.0.mp4")
)

// 将命令发送出去
EsMessenger.get().sendCommand(context, device, cmd)
```

#### 6.2 关闭快应用
``` kotlin
// 关闭包名为pkg1的快应用
val cmd = EsCommand.makeCmdCloseCommand("pkg1")
// 关闭包名为pkg1和pkg2的快应用
val cmd = EsCommand.makeCmdCloseCommand("pkg1", "pkg2")
// 关闭所有快应用
val cmd = EsCommand.makeCmdCloseCommand("all")

EsMessenger.get().sendCommand(context, device, cmd)
```

### <span id="j_remote_control">7 发送遥控器事件</span>
由于权限问题，只能当应用在前台的时候才能生效
``` kotlin
val cmd = EsCommand.makeCmdKeyEventCommand(KeyEvent.KEYCODE_VOLUME_UP)// 音量上键
EsMessenger.get().sendCommand(this, selectDevice, cmd)
```
**常用键值说明:**

|键值|3| 4 |19 |20| 21 |22| 23 |24 |25 |82
|-|-|-|-|-|-|-|-|-|-|-
|说明 |主界面| 返回| 上| 下 |左| 右| 确定| 音量+ |音量-| 菜单

[@更多键值](KeyEvent.java) 请参考`KEYCODE_xxxx`

### 8 发送自定义事件
``` java

// 发送
EsCommand.CmdArgs args = new EsCommand.CmdArgs("player")
                .put("action", "play");
EsCommand cmd = EsCommand.makeCustomCommand("OnLinkEvent") // 事件名称
                .setEventData(args)
                
EsMessenger.get().sendCommand(context, device, cmd)
                
// 接收
// MessengerCallback回调
public void onReceiveEvent(EsEvent event) {
}
```
**<span id="j_event">EsEvent说明:</span>**

| 方法 | 说明 |
|-|-|
|getDeviceIp|获取事件发送设备的IP
|getDevicePort|获取事件发送设备的端口
|getData|获取事件内容
