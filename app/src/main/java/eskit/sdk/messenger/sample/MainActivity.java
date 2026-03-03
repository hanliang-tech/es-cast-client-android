package eskit.sdk.messenger.sample;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eskit.sdk.support.messenger.client.EsMessenger;
import eskit.sdk.support.messenger.client.IEsMessenger;
import eskit.sdk.support.messenger.client.bean.EsDevice;
import eskit.sdk.support.messenger.client.bean.EsEvent;
import eskit.sdk.support.messenger.client.core.EsCommand;

public class MainActivity extends AppCompatActivity implements IEsMessenger.MessengerCallback, AdapterView.OnItemSelectedListener {

    private static final String TAG = "[-MainActivity-]";

    private static final String START_APP_PKG = "es.hello.world";

    private ArrayAdapter<EsDevice> mDeviceAdapter;
    private final List<EsDevice> mDevices = new ArrayList<>();
    private EsDevice mCurrentSelectDevice;

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);

        mDeviceAdapter = new ArrayAdapter<EsDevice>(this, android.R.layout.simple_spinner_item){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(mDevices.get(position).getDeviceName());
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setText((position + 1) + " " + mDevices.get(position).getDeviceName());
                return view;
            }
        };
        mDeviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner deviceSpinner = findViewById(R.id.deviceList);
        deviceSpinner.setAdapter(mDeviceAdapter);
        deviceSpinner.setOnItemSelectedListener(this);

        // --------------------------------------------------- //

        // 设置SDK回调
        EsMessenger.get().setMessengerCallback(this);

        EsMessenger.get().setOAID("123");
        EsMessenger.get().setAAID("456");
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
    public void onPingResponse(String deviceIp, int devicePort) {
        mHandler.post(() -> {
            Toast.makeText(this, "设备" + deviceIp +":" + devicePort + "在线", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onFindDevice(EsDevice device) {
        Log.i(TAG, "onFindDevice " + device);
        mDevices.add(device);
        Collections.sort(mDevices, (o1, o2) -> (int) (o1.getFindTime() - o2.getFindTime()));

        mHandler.post(() -> {
            if (mDeviceAdapter != null) {
                mDeviceAdapter.clear();
                mDeviceAdapter.addAll(mDevices);
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
        mCurrentSelectDevice = mDevices.get(position);
        Log.i(TAG, "onItemSelected " + mCurrentSelectDevice);
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
        if (mDeviceAdapter != null) {
            mDeviceAdapter.clear();
        }
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
     * 检测设备是否在线
     * @param view
     */
    public void pingDevice(View view) {
        if (mCurrentSelectDevice == null) return;
        EsMessenger.get().ping(this, mCurrentSelectDevice);
    }

    /**
     * 启动快应用
     **/
    public void startEsApp(View view) {
        if (mCurrentSelectDevice == null) return;
        EsMessenger.get().sendCommand(this, mCurrentSelectDevice, EsCommand.makeEsAppCommand(START_APP_PKG).setDebug(true));
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
     * 启动快应用
     **/
    public void startNativeApp(View view) {
        if (mCurrentSelectDevice == null) return;
        EsMessenger.get().sendCommand(this, mCurrentSelectDevice, EsCommand.makeNativeAppCommand("com.tcl.appmarket2").setDebug(true));
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
