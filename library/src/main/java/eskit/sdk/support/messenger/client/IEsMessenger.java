package eskit.sdk.support.messenger.client;

import android.content.Context;

import eskit.sdk.support.messenger.client.bean.EsDevice;
import eskit.sdk.support.messenger.client.bean.EsEvent;
import eskit.sdk.support.messenger.client.core.EsCommand;

/**
 *
 */
public interface IEsMessenger {

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
     * 设置用户Android设备的OAID
     * @param OAID 用户Android设备的OAID
     */
    void setOAID(String OAID);

    /**
     * 设置用户Android设备的AAID
     * @param AAID 用户Android设备的AAID
     */
    void setAAID(String AAID);

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
