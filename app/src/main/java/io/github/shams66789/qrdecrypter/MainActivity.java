package io.github.shams66789.qrdecrypter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String ACTION_USB_PERMISSION = "io.github.shams66789.qrdecrypter.USB_PERMISSION";

    private Button scanBtn, sendBtn;
    private TextView messageText, messageFormat;

    private UsbManager usbManager;
    private UsbAccessory usbAccessory;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbEndpoint usbEndpoint;

    private ParcelFileDescriptor pfd;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanBtn = findViewById(R.id.scanBtn);
        sendBtn = findViewById(R.id.button);
        messageText = findViewById(R.id.textContent);
        messageFormat = findViewById(R.id.textFormat);

        // Set OnClickListener for the buttons
        scanBtn.setOnClickListener(this);
        sendBtn.setOnClickListener(this);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);

        checkAccessory();
    }

    @Override
    public void onClick(View v) {
        // Handle button click
        if (v.getId() == R.id.scanBtn) {
            // Start the QR code scanner
            IntentIntegrator intentIntegrator = new IntentIntegrator(this);
            intentIntegrator.setPrompt("Scan a barcode or QR Code");
            intentIntegrator.setOrientationLocked(true);
            intentIntegrator.initiateScan();
        } else if (v.getId() == R.id.button) {
            // Send data to the computer
            sendToComputer();
        }
    }

    private void sendToComputer() {
        String dataToSend = messageText.getText().toString();

        if (usbDeviceConnection != null && usbEndpoint != null) {
            byte[] data = dataToSend.getBytes(StandardCharsets.UTF_8);

            try {
                // Open the output stream from the accessory
                FileOutputStream outputStream = new FileOutputStream(pfd.getFileDescriptor());

                // Write the data to the output stream
                outputStream.write(data);
                outputStream.flush();

                // Close the output stream
                outputStream.close();

                Toast.makeText(this, "Data sent successfully", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to send data", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "USB connection not established", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        // if the intentResult is null then
        // toast a message as "cancelled"
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                // if the intentResult is not null we'll set
                // the content and format of scan message
                messageText.setText(intentResult.getContents());
                messageFormat.setText(intentResult.getFormatName());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        usbAccessory = accessory;
                        // Permission granted, you can now open a connection and communicate with the USB accessory.
                        openUsbAccessoryConnection();
                    } else {
                        // Permission denied, handle accordingly.
                        Toast.makeText(MainActivity.this, "USB permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    private void checkAccessory() {
        UsbAccessory[] accessories = usbManager.getAccessoryList();
        UsbAccessory accessory = (accessories != null && accessories.length > 0) ? accessories[0] : null;

        if (accessory != null) {
            // An accessory is already connected, check if permission is granted
            if (usbManager.hasPermission(accessory)) {
                usbAccessory = accessory;
                // Permission granted, you can now open a connection and communicate with the USB accessory.
                openUsbAccessoryConnection();
            } else {
                // Request permission
                PendingIntent permissionIntent = PendingIntent.getBroadcast(
                        this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(accessory, permissionIntent);
            }
        }
    }

    private void openUsbAccessoryConnection() {
        UsbAccessory[] accessoryList = usbManager.getAccessoryList();
        UsbAccessory accessory = (accessoryList == null ? null : accessoryList[0]);
        if (accessory != null) {
            ParcelFileDescriptor fileDescriptor = usbManager.openAccessory(accessory);
            if (fileDescriptor != null) {
                FileDescriptor fd = fileDescriptor.getFileDescriptor();
                FileInputStream inputStream = new FileInputStream(fd);
                FileOutputStream outputStream = new FileOutputStream(fd);
                // Now you can use inputStream and outputStream to communicate with the accessory
                Toast.makeText(this, "USB accessory connection opened", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to open USB accessory connection", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
