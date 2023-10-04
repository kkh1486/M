package com.example.m;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;


// USB 장치가 연결되었을 때의 이벤트를 처리하고, 장치로부터 IMU 정보를 받아서 UsbDataReceiver 인터페이스를 통해 해당 정보를 전달하는 역할

// 브로드캐스트 리시버를 상속받는 UsbDeviceBroadcastReceiver 클래스 선언
public class UsbDeviceBroadcastReceiver extends BroadcastReceiver {
    private UsbDataReceiver dataReceiver;  // UsbDataReceiver 인터페이스를 참조하는 멤버 변수

    // 생성자
    // 이 생성자를 호출하면서 UsbDataReceiver 인스턴스를 매개변수로 전달하면
    // 해당 인스턴스를 멤버 변수에 저장합니다.
    public UsbDeviceBroadcastReceiver(UsbDataReceiver dataReceiver) {
        this.dataReceiver = dataReceiver;
    }

    // 브로드캐스트 메시지를 받았을 때 실행되는 메소드
    @Override
    public void onReceive(Context context, Intent intent) {
        // 동작 가져오기
        String action = intent.getAction();
        // USB 장치 가져오기
        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        // USB 권한 요청에 대한 응답인 경우
        if (MainActivity.ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                // 사용자가 권한을 승인했는지 확인
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    // 장치가 연결되어 있으면 장치에 연결하려고 시도
                    if(device != null){
                        ((MainActivity)dataReceiver).connectToDevice();
                    }
                }
                else {
                    // 권한이 거부된 경우
                    Log.d("USB", "permission denied for device " + device);
                }
            }
        }
        // USB 장치가 분리된 경우
        else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            // 장치가 연결되어 있으면 연결해제
            if (device != null) {
                ((MainActivity) dataReceiver).disconnectFromDevice();
            }
        }
    }

}
