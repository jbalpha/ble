package com.maxmade.bluetooth.le;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class DeviceScanActivity extends Activity implements OnTouchListener{
    private final static String DEBUG_TAG = "Debug";
    private static  final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION  = 100;

    private BluetoothLeScanner mScanner;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private ListView mBleDeviceListView;
    private LinearLayout mScanLayout;
    private ProgressBar mProgressBar;
    private TextView mScanState;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;

    private ExitReceiver exitReceiver = new ExitReceiver();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ble_scan_control);
        mScanLayout = (LinearLayout)findViewById(R.id.layout_scan);
        mScanLayout.setOnTouchListener(this);
        mProgressBar = (ProgressBar)findViewById(R.id.scan_progress_bar);
        mProgressBar.setVisibility(ProgressBar.GONE);
        mScanState = (TextView)findViewById(R.id.scan_status);
        mHandler = new Handler();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.bluetooth.le.ACTION_EXIT");
        registerReceiver(exitReceiver,filter);


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mScanner=mBluetoothAdapter.getBluetoothLeScanner();
        // Initializes list view adapter.
        mBleDeviceListView = (ListView) this.findViewById(R.id.listView);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mBleDeviceListView.setAdapter(mLeDeviceListAdapter);
        mBleDeviceListView.setOnItemClickListener(mDeviceListOnItemClick);
        checkBluetoothPermission();
        scanLeDevice(true);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mBleDeviceListView = (ListView) this.findViewById(R.id.listView);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mBleDeviceListView.setAdapter(mLeDeviceListAdapter);
        mBleDeviceListView.setOnItemClickListener(mDeviceListOnItemClick);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(exitReceiver);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                mScanLayout.setBackgroundColor(Color.parseColor("#00A0A0A0"));
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                mScanLayout.setBackgroundColor(Color.parseColor("#00D0D0D0"));
                if (!mScanning)
                {
                    mLeDeviceListAdapter.clear();
                    scanLeDevice(true);
                }
                else
                {
                    scanLeDevice(false);
                }
                break;
        }
        return true;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mScanner.stopScan(mLeScanCallback);

                    mScanState.setText(R.string.scan);
                    mProgressBar.setVisibility(ProgressBar.GONE);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mScanner.startScan(mLeScanCallback);
            mScanState.setText(R.string.stop);
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
        } else {
            mScanning = false;
            mScanner.stopScan(mLeScanCallback);
            mScanState.setText(R.string.scan);
            mProgressBar.setVisibility(ProgressBar.GONE);
        }
    }

    private void DeviceListClickPro(int item){
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(item);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mScanner.stopScan(mLeScanCallback);
            mScanning = false;
            mScanState.setText(R.string.scan);
            mProgressBar.setVisibility(ProgressBar.GONE);
        }
        startActivity(intent);
    }

    AdapterView.OnItemClickListener mDeviceListOnItemClick = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
            DeviceListClickPro(arg2);
        }
    };

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    private class ExitReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(action.equals("com.example.bluetooth.le.ACTION_EXIT"))
            {
                DeviceScanActivity.this.finish();
            }
        }
    }


    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }


    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType,
                                         ScanResult result) {
                    super.onScanResult(callbackType, result);

                    final BluetoothDevice device=result.getDevice();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    private void checkBluetoothPermission(){
        if (Build.VERSION.SDK_INT <23) return;
        if (ContextCompat.checkSelfPermission(DeviceScanActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //判断是否需要向用户解释为何要此权限
            if (!ActivityCompat.shouldShowRequestPermissionRationale(DeviceScanActivity.this,
                    Manifest.permission.READ_CONTACTS)) {
                showMessageOKCancel("You need to allow access to Contacts", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(DeviceScanActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                    }
                });
                Log.i(DEBUG_TAG,"Not , ACCESS_COARSE_LOCATION!");
                return;
            }
            //请求权限
            Log.i(DEBUG_TAG,"Ok,  WRITE_CONTACTS!");

            requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(DeviceScanActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //同意权限,做跳转逻辑
                    connectBluetooth();
                } else {
                    // 权限拒绝，提示用户开启权限
                    denyPermission();
                }
            }
            break;
            default:
                break;
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    private void denyPermission() {
       // Toast.makeText(DeviceScanActivity.this,"Bluetooth is denied！",Toast.LENGTH_SHORT).show();
        Log.i(DEBUG_TAG,"deny,  bluetooth fail");
    }

    private void connectBluetooth() {
        //Toast.makeText(DeviceScanActivity.this,"Succeed in connecting bluetooth！",Toast.LENGTH_SHORT).show();
        Log.i(DEBUG_TAG,"OK,  connect with bluetooth ");
    }



}
