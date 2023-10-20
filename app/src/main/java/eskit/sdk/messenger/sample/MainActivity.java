package eskit.sdk.messenger.sample;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import eskit.sdk.support.messenger.client.EsMessenger;
import eskit.sdk.support.messenger.client.IEsMessenger;
import eskit.sdk.support.messenger.client.bean.EsDevice;
import eskit.sdk.support.messenger.client.bean.EsEvent;
import eskit.sdk.support.messenger.client.core.EsCommand;

public class MainActivity extends AppCompatActivity implements IEsMessenger.MessengerCallback, AdapterView.OnItemSelectedListener {

    private static final String START_APP_PKG = "es.hello.world";

    private ArrayAdapter<String> mDeviceAdapter;
    private final Map<String, EsDevice> mDevices = new LinkedHashMap<>();
    private EsDevice mCurrentSelectDevice;

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);

        mDeviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        mDeviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner deviceSpinner = findViewById(R.id.deviceList);
        deviceSpinner.setAdapter(mDeviceAdapter);
        deviceSpinner.setOnItemSelectedListener(this);

        // --------------------------------------------------- //

        // 设置SDK回调
        EsMessenger.get().setMessengerCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 停止并释放资源
        EsMessenger.get().stop();

        mDevices.clear();
        if (mDeviceAdapter != null) {
            mDeviceAdapter.clear();
            mDeviceAdapter = null;
        }
        mCurrentSelectDevice = null;

        mHandler.removeCallbacksAndMessages(null);
    }

    //region SDK回调

    @Override
    public void onFindDevice(EsDevice device) {
        mDevices.put(device.getDeviceName(), device);

        mHandler.post(() -> {
            if (mDeviceAdapter != null) {
                mDeviceAdapter.clear();
                mDeviceAdapter.addAll(mDevices.keySet());
                mDeviceAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void onReceiveEvent(EsEvent event) {
        mHandler.post(() -> {
            Toast.makeText(this, event.getData(), Toast.LENGTH_SHORT).show();
        });
    }

    //endregion

    //region 设备选择

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        if (mDevices.size() == 0) return;
        Set<String> names = mDevices.keySet();
        int index = 0;
        for (String name : names) {
            if (index++ == position) {
                mCurrentSelectDevice = mDevices.get(name);
                break;
            }
        }
        Log.i("AAA", "" + mCurrentSelectDevice);
        findViewById(R.id.visible_group).setVisibility(View.VISIBLE);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        findViewById(R.id.visible_group).setVisibility(View.INVISIBLE);
    }

    //endregion

    //region 按钮事件

    /**
     * 搜索
     **/
    public void startSearch(View view) {
        mDevices.clear();
        EsMessenger.get().search(this);
    }

    /**
     * 停止
     **/
    public void stopSearch(View view) {
        EsMessenger.get().stop();

        if (mDeviceAdapter != null) {
            mDeviceAdapter.clear();
            mDeviceAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 启动快应用
     **/
    public void startEsApp(View view) {
        if (mCurrentSelectDevice == null) return;
        EsMessenger.get().sendCommand(this, mCurrentSelectDevice, EsCommand.makeEsAppCommand(START_APP_PKG));
    }

    /**
     * 关闭快应用
     **/
    public void closeEsApp(View view) {
        if (mCurrentSelectDevice == null) return;
        EsMessenger.get().sendCommand(this, mCurrentSelectDevice, EsCommand.makeCmdCloseCommand(START_APP_PKG));
    }

    /**
     * 音量+
     **/
    public void setVolumeUp(View view) {
        if (mCurrentSelectDevice == null) return;
        EsMessenger.get().sendCommand(this, mCurrentSelectDevice, EsCommand.makeCmdKeyEventCommand(KeyEvent.KEYCODE_VOLUME_UP));
    }

    /**
     * 音量-
     **/
    public void setVolumeDown(View view) {
        if (mCurrentSelectDevice == null) return;
        EsMessenger.get().sendCommand(this, mCurrentSelectDevice, EsCommand.makeCmdKeyEventCommand(KeyEvent.KEYCODE_VOLUME_DOWN));
    }

    //endregion


}
