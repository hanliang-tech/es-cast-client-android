package eskit.sdk.messenger.sample;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import eskit.sdk.support.messenger.client.bean.EsDevice;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private MainViewModel mViewModel;

    private ArrayAdapter<EsDevice> mDeviceAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mDeviceAdapter = new ArrayAdapter<EsDevice>(this, android.R.layout.simple_spinner_item) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(getItem(position).getDeviceName());
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setText((position + 1) + " " + getItem(position).getDeviceName());
                return view;
            }
        };
        mDeviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner deviceSpinner = findViewById(R.id.deviceList);
        deviceSpinner.setAdapter(mDeviceAdapter);
        deviceSpinner.setOnItemSelectedListener(this);

        mViewModel = new MainViewModel((Application) getApplicationContext());
        observeData();
    }

    private void observeData() {

        mViewModel.deviceListData.observe(this, deviceList -> {
            if (mDeviceAdapter != null) {
                mDeviceAdapter.clear();
                if (deviceList != null) {
                    mDeviceAdapter.addAll(deviceList);
                }
                mDeviceAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.onDestroy();
        if (mDeviceAdapter != null) {
            mDeviceAdapter.clear();
            mDeviceAdapter = null;
        }
    }

    //region 设备选择

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        List<EsDevice> deviceList = mViewModel.deviceListData.getValue();
        if (deviceList == null || deviceList.size() == 0) return;
        EsDevice device = deviceList.get(position);
        mViewModel.setCurrentDevice(device);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        mViewModel.setCurrentDevice(null);
    }

    //endregion

    //region 按钮事件


    @SuppressLint("NonConstantResourceId")
    public void onButtonClick(View view) {
        int id = view.getId();

        switch (id) {
            // 搜索
            case R.id.btn_start_search:
                mViewModel.stopSearch();
                mViewModel.startSearch();
                break;
            // 停止搜索
            case R.id.btn_stop_search:
                mViewModel.stopSearch();
                break;
            // 设备是否在线
            case R.id.btn_ping_device:
                mViewModel.pingDevice();
                break;
            // 音量+
            case R.id.btn_set_volume_up:
                mViewModel.setVolumeUp();
                break;
            // 音量-
            case R.id.btn_set_volume_down:
                mViewModel.setVolumeDown();
                break;
            // 启动首页
            case R.id.btn_start_home_page:
                mViewModel.startHomePage();
                break;
            // 启动首页并传参
            case R.id.btn_start_home_page_with_params:
                mViewModel.startHomePageWithParams();
                break;
            // 关闭应用
            case R.id.btn_close_app:
                mViewModel.closeEsApp();
                break;
            // 启动播放页
            case R.id.btn_start_player_page:
                mViewModel.startPlayerPage();
                break;
            // 启动播放页并传参
            case R.id.btn_start_player_page_with_params:
                mViewModel.startPlayerPageWithParams();
                break;
            // 播放
            case R.id.btn_player_play:
                mViewModel.play();
                break;
            // 暂停
            case R.id.btn_player_pause:
                mViewModel.pause();
                break;
            // 快进
            case R.id.btn_player_forward:
                mViewModel.forward();
                break;
            // 快退
            case R.id.btn_player_backward:
                mViewModel.backward();
                break;
        }
    }

    //endregion


}
