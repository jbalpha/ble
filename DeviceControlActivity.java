/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maxmade.bluetooth.le;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.UUID;

/**
 * 对于一个BLE设备，该activity向用户提供设备连接，显示数据，显示GATT服务和设备的字符串支持等界面，
 * 另外这个activity还与BluetoothLeService通讯，反过来与Bluetooth LE API进行通讯
 */
public class DeviceControlActivity extends Activity implements OnTouchListener{
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    
    private final static byte HEAD_CODE_0 = (byte)0xA0;
    private final static byte HEAD_CODE_1 = (byte)0x55;
    private final static byte GID_GENERAL_CMD = (byte)0x01;
    private final static byte UICC_HOME = (byte)0x01;
    private final static byte UICC_BACK = (byte)0x02;
    private final static byte UICC_DIAL = (byte)0x03;
    private final static byte UICC_HUNGUP = (byte)0x04;
    private final static byte UICC_UP = (byte)0x05;
    private final static byte UICC_DOWN = (byte)0x06;
    private final static byte UICC_LEFT = (byte)0x07;
    private final static byte UICC_RIGHT = (byte)0x08;
    private final static byte UICC_VOLUME_CW = (byte)0x09;
    private final static byte UICC_VOLUME_CCW = (byte)0x0A;
    private final static byte UICC_OK = (byte)0x0B;

    //连接状态
    private String mDeviceName;
    private String mDeviceAddress;
    
    private ImageView ivVolume;
    private LinearLayout mKeyBoardLayout;
    private LinearLayout mConnectLayout;
    private TextView mConnectState; 
    private ProgressBar mProgressBar;
    
	float angle=0;
	float start_angle;
	float diff_angle;
	int positionX;
	int positionY;
	int startX;
	int startY;
	int centerX;
	int centerY;
	double length_oa;
	double length_ob;
	double length_ab;
	int ivVolumeWidth;
	int ivVolumeHeight;
	int ivVolumePosition[] = new int[2];
	int direction;
	double move_angle;
	boolean keyboard_show_flag = false;
	boolean keyboard_draw_flag = false;
	ViewTreeObserver vto;    
    
    private BluetoothLeService mBluetoothLeService;
  
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    //写数据
    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattService mnotyGattService;;
    //读数据
    private BluetoothGattCharacteristic readCharacteristic;
    private BluetoothGattService readMnotyGattService;
    byte[] WriteBytes = new byte[20];

    // 管理服务的生命周期
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.处理服务所激发的各种事件
    // ACTION_GATT_CONNECTED: connected to a GATT server.连接一个GATT服务
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.从GATT服务中断开连接
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.查找GATT服务
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.从服务中接受数据
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mConnectState.setText(R.string.connected);
                showLayout_KeyBoard();
                
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mConnectState.setText(R.string.disconnected);
            } 
            //发现有可支持的服务
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            	//写数据的服务和characteristic
            	//mnotyGattService = mBluetoothLeService.getSupportedGattServices(UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb"));
            	mnotyGattService = mBluetoothLeService.getSupportedGattServices(UUID.fromString("1706BBC0-88AB-4B8D-877E-2237916EE929"));        	
                characteristic = mnotyGattService.getCharacteristic(UUID.fromString("BD28E457-4026-4270-A99F-F9BC20182E15"));
                //读数据的服务和characteristic
                readMnotyGattService = mBluetoothLeService.getSupportedGattServices(UUID.fromString("1706BBC0-88AB-4B8D-877E-2237916EE929"));
                readCharacteristic = readMnotyGattService.getCharacteristic(UUID.fromString("BD28E457-4026-4270-A99F-F9BC20182E15"));
            } 
            //显示数据
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	//将数据显示在mDataField上
            	String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
            	System.out.println("data----" + data);
            }
        }
    };    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_run_control);
        InitView();
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }
    
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				startX = (int)event.getRawX();
				startY = (int)event.getRawY();
				start_angle = angle;
				break;
			case MotionEvent.ACTION_MOVE:
				positionX = (int)event.getRawX();
				positionY = (int)event.getRawY();	
				if(positionX!=startX
						||positionY!=startY)
				{
					length_oa = (centerX-startX)*(centerX-startX)+(centerY-startY)*(centerY-startY);
					length_oa = (int)Math.sqrt((double)length_oa);
					length_ob = (centerX-positionX)*(centerX-positionX)+(centerY-positionY)*(centerY-positionY);
					length_ob = (int)Math.sqrt((double)length_ob);	
					length_ab = (startX-positionX)*(startX-positionX)+(startY-positionY)*(startY-positionY);
					length_ab = (int)Math.sqrt((double)length_ab);	
					direction = (startX-centerX)*(positionY-centerY)-(startY-centerY)*(positionX-centerX);
					double temp = (length_oa*length_oa+length_ob*length_ob-length_ab*length_ab)/(2*length_oa*length_ob);
					move_angle = Math.acos((double)temp);
					move_angle = (move_angle*180)/3.14;
					if(Math.abs(move_angle)>5)
					{
						if(direction>0)
						{
							angle += move_angle;
						}
						else
						{
							angle -= move_angle;
						}
						if(angle>360)
						{
							angle -= 360;
						}
						else if(angle<-360)
						{
							angle += 360;
						}
						startX = positionX;
						startY = positionY;
						ivVolume.setPivotX(ivVolumeWidth/2);
						ivVolume.setPivotY(ivVolumeHeight/2);
						ivVolume.setRotation(angle);
					}
					
					byte send_cmd = '0';
					if(angle>start_angle)
					{
						diff_angle = angle - start_angle;
						send_cmd = UICC_VOLUME_CW;
					}
					else
					{
						diff_angle = start_angle-angle;
						send_cmd = UICC_VOLUME_CCW;
					}
					if(diff_angle>10)
					{
						start_angle = angle;
						sendCmdMessage(send_cmd);
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				break;
		}
		return true;
	}
    /*
	 * **************************************************************
	 * *****************************读函数*****************************
	 */
    
    private void InitView()
    {
        mKeyBoardLayout = (LinearLayout)findViewById(R.id.ll_keyboard);
        mConnectLayout = (LinearLayout)findViewById(R.id.ll_connect);
        mConnectState = (TextView)findViewById(R.id.connection_state);  
        mProgressBar = (ProgressBar)findViewById(R.id.connect_progress_bar);
        showLayout_connect();
		ivVolume = (ImageView) findViewById(R.id.bt_volume);
		ivVolume.setOnTouchListener(this);
    }
    
	private void showLayout_KeyBoard() 
	{
		mConnectLayout.clearAnimation();
		mConnectLayout.setVisibility(View.GONE);
		mKeyBoardLayout.clearAnimation();
		mKeyBoardLayout.setVisibility(View.VISIBLE);
		if(keyboard_show_flag==false)
		{
			ViewTreeObserver vto = ivVolume.getViewTreeObserver();
			vto.addOnPreDrawListener(new OnPreDrawListener() {
				@Override
				public boolean onPreDraw() {
					if(keyboard_draw_flag==false)
					{
						ivVolumeHeight = ivVolume.getMeasuredHeight();
						ivVolumeWidth = ivVolume.getMeasuredWidth();
						ivVolume.getLocationOnScreen(ivVolumePosition);
						centerX = ivVolumePosition[0]+ivVolumeWidth/2;
						centerY = ivVolumePosition[1]+ivVolumeHeight/2;
						keyboard_draw_flag=true;
					}
					return true;
				}
			});
			keyboard_show_flag = true;
		}
	}
	
	private void showLayout_connect() 
	{
		mKeyBoardLayout.clearAnimation();
		mKeyBoardLayout.setVisibility(View.GONE);
		mConnectLayout.clearAnimation();
		mConnectLayout.setVisibility(View.VISIBLE);
	}
	
    private void read() {
    	mBluetoothLeService.setCharacteristicNotification(readCharacteristic, true);
	}
    
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
	public void OnControlClick(View v) 
	{
		boolean flag = false;
		byte cmd = 0; 
		
		switch (v.getId()) 
		{
			case R.id.bt_home:
				cmd = UICC_HOME;
				flag = true;
				break;
			case R.id.bt_back:
				cmd = UICC_BACK;
				flag = true;
				break;
			case R.id.bt_dial:
				cmd = UICC_DIAL;
				flag = true;
				break;
			case R.id.bt_setup:
				Intent setup_intent = new Intent(this, SettingActivity.class);
				startActivity(setup_intent); 
				break;
			case R.id.bt_hungup:
				cmd = UICC_HUNGUP;
				flag = true;
				break;
			case R.id.bt_up:
				cmd = UICC_UP;
				flag = true;
				break;
			case R.id.bt_left:
				cmd = UICC_LEFT;
				flag = true;
				break;
			case R.id.bt_rigth:
				cmd = UICC_RIGHT;
				flag = true;
				break;
			case R.id.bt_down:
				cmd = UICC_DOWN;
				flag = true;
				break;
			case R.id.bt_ok:
				cmd = UICC_OK;
				flag = true;
				break;
			case R.id.bt_connect:
				mBluetoothLeService.connect(mDeviceAddress);
				mConnectState.setText(R.string.connectting);
				mProgressBar.setVisibility(ProgressBar.VISIBLE);
				break;
			case R.id.bt_disconnect:
				mBluetoothLeService.disconnect();
				mConnectState.setText(R.string.disconnected);
				mProgressBar.setVisibility(ProgressBar.GONE);
				break;
			case R.id.bt_connect_back:
				onBackPressed();
				break;
			case R.id.bt_power:
				Intent intent = new Intent("com.example.bluetooth.le.ACTION_EXIT");
				getApplicationContext().sendBroadcast(intent);	
				finish();
				break;
			default:
				break;
		}
		if(flag==true)
		{
			sendCmdMessage(cmd);
		}
	}
	
	public void sendCmdMessage(byte cmd)
	{
		int i = 0;
		int checksum=0;	
		
		for(i=0;i<20;i++)
		{
			WriteBytes[i] = 0;
		}
		WriteBytes[0] = HEAD_CODE_0;
		WriteBytes[1] = HEAD_CODE_1;
		WriteBytes[2] = 0x05;
		WriteBytes[3] = GID_GENERAL_CMD;
		WriteBytes[4] = (byte)cmd;
		for(i=2;i<9;i++)
		{
			checksum += WriteBytes[i];
		}
		checksum = checksum^0xFF;
		checksum += 1;
		WriteBytes[9] = (byte)checksum;
		
		read();
        final int charaProp = characteristic.getProperties();
        
        //如果该char可写
        if((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) 
        {
            if(mNotifyCharacteristic != null) 
            {
                mBluetoothLeService.setCharacteristicNotification( mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            characteristic.setValue(WriteBytes);
            mBluetoothLeService.writeCharacteristic(characteristic);
        }
        if((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) 
        {
            mNotifyCharacteristic = characteristic;
            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
        }	
	}
}

