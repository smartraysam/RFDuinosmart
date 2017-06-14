package com.smarttechx.rfduino;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.UUID;



public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    // State machine
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;
    //private BluetoothGatt mBluetoothGatt;
    SeekBar customSeekbar;
    private TextView progress;
    private int state;
    private boolean scanStarted;
    private boolean scanning;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private RFduinoService rfduinoService;
    private TextView enableBluetooth;
    private TextView scanStatusText;
    private Button scanButton;
    private TextView deviceInfoText;
    private TextView connectionStatusText;
    private Button connectButton;
    private Button disconnectButton;
    private Button sendFourButton, closeApp;
    private Button sendTwelveButton;
    private Button sendTwentyButton;
    private TextView valOne;
    int count=0;
    String valget, val1,val2;



    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_ON) {
                upgradeState(STATE_DISCONNECTED);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                downgradeState(STATE_BLUETOOTH_OFF);
            }
        }
    };

    private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanning = (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
            scanStarted &= scanning;
            updateUi();
        }
    };

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize()) {
                if (rfduinoService.connect(bluetoothDevice.getAddress())) {
                    upgradeState(STATE_CONNECTING);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rfduinoService = null;
            downgradeState(STATE_DISCONNECTED);
        }
    };

    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                upgradeState(STATE_CONNECTED);
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                downgradeState(STATE_DISCONNECTED);
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
              // addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
                String hex = HexAsciiHelper.bytesToHex(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
                if (count<2 && hex!="41"){
                    count++;
                    String ascii=hexToAscii(hex);
                    valget=valget+ascii;
                    //addDatastr(valget);
                }
                else{
                    addDatastr(valget);
                    count=0;
                    valget="";
                }

            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Bluetooth
        // Find Device
        scanStatusText = (TextView) findViewById(R.id.scanStatus);
        deviceInfoText = (TextView) findViewById(R.id.deviceInfo);
        customSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        progress = (TextView) findViewById(R.id.valueIn);
        enableBluetooth = (TextView) findViewById(R.id.enableBluetoothButton);

        // Connect Device
        connectionStatusText = (TextView) findViewById(R.id.connectionStatus);
        scanButton = (Button) findViewById(R.id.scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothAdapter.enable();
                scanStarted = true;
                bluetoothAdapter.startLeScan(
                        new UUID[]{RFduinoService.UUID_SERVICE},
                        MainActivity.this);

            }
        });


        closeApp = (Button) findViewById(R.id.Exit);
        closeApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                System.exit(0);
            }
        });
        // Device Info

        disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                scanStarted = false;
                disconnectButton.setEnabled(false);
                if(rfduinoService==null){
                    bluetoothAdapter.disable();

                }
                else{
                   // rfduinoService.close();
                    rfduinoService.disconnect();
                    bluetoothAdapter.disable();
                }

            }
        });

        connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(false);

                connectionStatusText.setText("Connecting...");
                Intent rfduinoIntent = new Intent(MainActivity.this, RFduinoService.class);
                bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
                scanStarted = false;
            }
        });

        sendFourButton = (Button) findViewById(R.id.sendFour);
        sendFourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rfduinoService.send(String.valueOf(4).getBytes());
                //Toast.makeText(MainActivity.this, String.valueOf(4), Toast.LENGTH_SHORT).show();
            }
        });
        sendTwelveButton = (Button) findViewById(R.id.sendTwelve);
        sendTwelveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //byte[] value = new byte[2];
                //value[0] = (byte) ((1) & 0xFF);
                //value[1] = (byte) ((2) & 0xFF);
                //rfduinoService.send(value);
                rfduinoService.send(String.valueOf(12).getBytes());
            }
        });
        sendTwentyButton = (Button) findViewById(R.id.sendTwenty);
        sendTwentyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // byte[] value = new byte[2];
                // value[0] = (byte) ((2) & 0xFF);
                //value[1] = (byte) ((0) & 0xFF);
                //rfduinoService.send(value
                rfduinoService.send(String.valueOf(20).getBytes());
                // Toast.makeText(MainActivity.this, String.valueOf(value), Toast.LENGTH_SHORT).show();
                //Toast.makeText(MainActivity.this, String.valueOf(String.valueOf(20).getBytes()), Toast.LENGTH_SHORT).show();

            }
        });
        customSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int out = i + 4;
                progress.setText(" " + String.valueOf(out) + "mA");
                if (connectionStatusText.getText().toString() == "Disconnected") {

                } else {
                    //Toast.makeText(MainActivity.this, String.valueOf(out), Toast.LENGTH_SHORT).show();
                    rfduinoService.send(String.valueOf(out).getBytes());
                    //byte[] value = new byte[2];
                    //value[0] = (byte) (Integer.parseInt(String.valueOf(out)) & 0xFF);
                    //value[1] = (byte) (Integer.parseInt(String.valueOf(out)) & 0xFF);
                    //rfduinoService.send(value);

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(scanModeReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());

        updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetoothAdapter.stopLeScan(this);
        unregisterReceiver(scanModeReceiver);
        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(rfduinoReceiver);
    }


    private void upgradeState(int newState) {
        if (newState > state) {
            updateState(newState);
        }
    }

    private void downgradeState(int newState) {
        if (newState < state) {
            updateState(newState);
        }
    }

    private void updateState(int newState) {
        state = newState;
        updateUi();
    }

    private void updateUi() {
        // Enable Bluetooth
        boolean on = state > STATE_BLUETOOTH_OFF;
        scanButton.setEnabled(on);
        if (bluetoothAdapter.isEnabled() == true) {
            enableBluetooth.setText("Bluetooth ON");
            disconnectButton.setEnabled(true);
            scanButton.setEnabled(false);
        } else {
            enableBluetooth.setText("Bluetooth OFF");
        }
        // Scan
        if (scanStarted && scanning) {
            scanStatusText.setText("Scanning...");
            scanButton.setText("Stop Scan");
            scanButton.setEnabled(true);

        } else if (scanStarted) {
            scanStatusText.setText("Scan started...");
            scanButton.setEnabled(false);
        } else {
            scanStatusText.setText("");
            scanButton.setText("Scan");
            scanButton.setEnabled(true);
        }


        // Connect
        boolean connected = false;
        String connectionText = "Disconnected";
        if (state == STATE_CONNECTING) {
            connectionText = "Connecting...";
        } else if (state == STATE_CONNECTED) {
            connected = true;
            //
            connectionText = "Connected";
        }
        connectionStatusText.setText(connectionText);
        connectButton.setEnabled(bluetoothDevice != null && state == STATE_DISCONNECTED);

        // Send

        sendFourButton.setEnabled(connected);
        sendTwelveButton.setEnabled(connected);
        sendTwentyButton.setEnabled(connected);

    }

    private void addData(byte[] data) {
       // String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
        valOne = (TextView) findViewById(R.id.valueone);
        String hex = HexAsciiHelper.bytesToHex(data);
        String ascii=hexToAscii(hex);
        valOne.setText(ascii+"."+"00mA");
    }
    private void addDatastr(String data) {
        // String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
        valOne = (TextView) findViewById(R.id.valueone);
        if(data.length()==4){
            val1=data.substring(0,2);
            val2=data.substring(2,4);
            valOne.setText(val1+"."+val2+"mA");
        }
        else
        {
            val1=data.substring(0,1);
            val2=data.substring(1);
            valOne.setText(val1+"."+val2+"mA");
        }

    }


    private  static   String hexToAscii(String hexString){
        int val = Integer.parseInt(hexString,16);
        return String.valueOf(val);
    }

    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceInfoText.setText(
                        BluetoothHelper.getDeviceInfoText(bluetoothDevice, rssi, scanRecord));
                updateUi();
            }
        });
    }
}

