package com.example.m;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.util.HashMap;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.CommandLong;
import io.dronefleet.mavlink.common.GpsRawInt;
import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.RawImu;
import io.dronefleet.mavlink.common.ScaledPressure;
import io.dronefleet.mavlink.common.Timesync;
import io.dronefleet.mavlink.minimal.Heartbeat;

public class MainActivity extends AppCompatActivity implements UsbDataReceiver {
    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private TextView connectionStatus;
    private TextView magneticX;
    private TextView magneticY;
    private TextView magneticZ;
    private TextView txtType;
    private TextView txtAutopilot;
    private TextView txtBaseMode;
    private TextView txtCustomMode;
    private TextView txtSystemStatus;
    private TextView txtMavlinkVersion;
    private TextView txtTimesyncTc1;
    private TextView txtTimesyncTs1;
    private TextView gpss;

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serial;
    private MavlinkConnection mavlinkConnection;

    private BroadcastReceiver usbReceiver;
    private PendingIntent permissionIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionStatus = findViewById(R.id.connectionStatus);
        magneticX = findViewById(R.id.magneticX);
        magneticY = findViewById(R.id.magneticY);
        magneticZ = findViewById(R.id.magneticZ);
        txtType = findViewById(R.id.heartbeat_type);
        txtAutopilot = findViewById(R.id.heartbeat_autopilot);
        txtBaseMode = findViewById(R.id.heartbeat_basemode);
        txtCustomMode = findViewById(R.id.heartbeat_custommode);
        txtSystemStatus = findViewById(R.id.heartbeat_systemstatus);
        txtMavlinkVersion = findViewById(R.id.heartbeat_mavlinkversion);
        txtTimesyncTc1 = findViewById(R.id.ts_tc1);
        txtTimesyncTs1 = findViewById(R.id.ts_ts1);
        gpss = findViewById(R.id.gps);

        // USB 관리자 초기화
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        // PendingIntent 초기화
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        // 인텐트 필터 및 브로드캐스트 리시버 설정
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        usbReceiver = new UsbDeviceBroadcastReceiver(this);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);

    }

    // USB 장치 목록을 확인하고 연결 시도
    @Override
    protected void onResume() {
        super.onResume();
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice dev : deviceList.values()) {
            if (dev.getVendorId() == 0x1209 && dev.getProductId() == 0x5741) {
                device = dev;
                break;
            }
        }

        if (device != null) {
            if (usbManager.hasPermission(device)) {
                connectToDevice();
            } else {
                usbManager.requestPermission(device, permissionIntent);
            }
        } else {
            connectionStatus.setText("Not connected");
        }
    }

    // 장치 연결 해제
    @Override
    protected void onPause() {
        super.onPause();
        disconnectFromDevice();
    }

    // 장치 연결
    public void connectToDevice() {
        // 장치가 null이 아닌지 확인
        if (device != null) {
            // 장치에 대한 권한이 있는지 확인
            if (usbManager.hasPermission(device)) {
                // 장치와의 연결을 열기
                connection = usbManager.openDevice(device);

                // 장치와 연결을 사용하여 직렬장치 생성
                serial = UsbSerialDevice.createUsbSerialDevice(device, connection);
                if (serial != null) {
                    // 직렬 장치가 성공적으로 열렸는지 확인
                    if (serial.open()) {
                        // 직렬 포트 설정
                        serial.setBaudRate(57600);  // 전송 속도 설정
                        serial.setDataBits(UsbSerialInterface.DATA_BITS_8); // 데이터 비트 설정
                        serial.setStopBits(UsbSerialInterface.STOP_BITS_1); // 정지 비트 설정
                        serial.setParity(UsbSerialInterface.PARITY_NONE);   // 패리티 설정
                        serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF); // 흐름 제어 설정

                        // 동기식으로 직렬 포트 열기
                        serial.syncOpen();

                        // Mavlink 연결 생성
                        mavlinkConnection = MavlinkConnection.create(serial.getInputStream(), serial.getOutputStream());

                        // Mavlink 데이터를 처리하기 위한 새 스레드 생성
                        new Thread(() -> {

/*                            int systemId = 255;
                            int componentId = 0;
                            CommandLong commandLong = CommandLong.builder()
                                    .targetSystem(1)
                                    .targetComponent(1)
                                    .command(MavCmd.MAV_CMD_SET_MESSAGE_INTERVAL)
                                    .param1(27) // RAW_IMU 메시지 ID
                                    .param2(10000000) // 10Hz (단위: 마이크로초)
                                    .confirmation(0)
                                    .build();

                            try {
                                mavlinkConnection.send2(systemId, componentId, commandLong);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }*/
                            request(27);
                            request(29);




                            while (!Thread.currentThread().isInterrupted()) {
                                try {
                                    MavlinkMessage message = mavlinkConnection.next();
/*                                    runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this, "Received message: " + message.getPayload().getClass().getSimpleName(), Toast.LENGTH_LONG).show();
                                    });*/
                                    // 메시지가 RawImu 형식인 경우
                                    if (message.getPayload() instanceof RawImu) {
                                        RawImu rawImu = (RawImu) message.getPayload();
                                        runOnUiThread(() -> {
                                            magneticX.setText(String.valueOf(rawImu.xmag()));
                                            magneticY.setText(String.valueOf(rawImu.ymag()));
                                            magneticZ.setText(String.valueOf(rawImu.zmag()));
                                        });
                                    }
                                    // 메시지가 Heartbeat 형식인 경우
                                    else if (message.getPayload() instanceof Heartbeat) {
                                        Heartbeat heartbeat = (Heartbeat) message.getPayload();
                                        runOnUiThread(() -> {
                                            //Toast.makeText(MainActivity.this, "Received Heartbeat: " + heartbeat.autopilot().entry(), Toast.LENGTH_LONG).show();
                                            txtType.setText("Type: " + heartbeat.type().entry());
                                            txtAutopilot.setText("Autopilot: " + heartbeat.autopilot().entry());
                                            txtBaseMode.setText("Base Mode: " + heartbeat.baseMode().entry());
                                            txtCustomMode.setText("Custom Mode: " + heartbeat.customMode());
                                            txtSystemStatus.setText("System Status: " + heartbeat.systemStatus().entry());
                                            txtMavlinkVersion.setText("MAVLink Version: " + heartbeat.mavlinkVersion());
                                        });
                                    }
                                    // 메시지가 Timesync 형식인 경우
                                    else if (message.getPayload() instanceof Timesync) {
                                        Timesync timesync = (Timesync) message.getPayload();
                                        runOnUiThread(() -> {
                                            //Toast.makeText(MainActivity.this, "Received Timesync: " + timesync.tc1(), Toast.LENGTH_LONG).show();
                                            txtTimesyncTc1.setText(String.valueOf(timesync.tc1()));
                                            txtTimesyncTs1.setText(String.valueOf(timesync.ts1()));
                                        });
                                    }
                                    // 메시지가 SCALED_PRESSURE 형식인 경우
                                    else if (message.getPayload() instanceof ScaledPressure) {
                                        ScaledPressure gpsrawint = (ScaledPressure) message.getPayload();
                                        runOnUiThread(() -> {
                                            gpss.setText(String.valueOf(gpsrawint.temperature()));
                                        });
                                    }
                                    else {
                                        runOnUiThread(() -> {
                                            Toast.makeText(MainActivity.this, "Received non-RawImu message: " + message.getPayload().getClass().getSimpleName(), Toast.LENGTH_LONG).show();
                                        });
                                    }
                                } catch (Exception e) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this, "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }).start();

                        runOnUiThread(() -> {
                            connectionStatus.setText("Connected");
                            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Failed to open serial port", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to create serial device", Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                // 권한이 없는 경우, 권한 요청
                PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(device, pi);
            }
        } else {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "Device not connected", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void request(int num) {
        int systemId = 255;
        int componentId = 0;
        CommandLong commandLong = CommandLong.builder()
                .targetSystem(1)
                .targetComponent(1)
                .command(MavCmd.MAV_CMD_SET_MESSAGE_INTERVAL)
                .param1(num) // 메시지 ID
                .param2(10000000) // 10Hz (단위: 마이크로초)
                .confirmation(0)
                .build();

        try {
            mavlinkConnection.send2(systemId, componentId, commandLong);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 장치 연결 해제
    public void disconnectFromDevice() {
        if (serial != null) {
            serial.close();
        }
        if (connection != null) {
            connection.close();
        }
        device = null;
        runOnUiThread(() -> {
            connectionStatus.setText("Not connected");
        });
    }

    // 앱 종료 시 브로드캐스트 리시버 해제 및 장치 연결 해제
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
        disconnectFromDevice();
    }

    // 데이터 수신 인터페이스
    @Override
    public void onReceivedData(double x, double y, double z) {
        runOnUiThread(() -> {
            magneticX.setText(String.valueOf(x));
            magneticY.setText(String.valueOf(y));
            magneticZ.setText(String.valueOf(z));
        });
    }

}
