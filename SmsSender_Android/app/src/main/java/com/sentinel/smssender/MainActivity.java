package com.sentinel.smssender;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 1001;
    private static final String PREFS_NAME = "SmsSenderPrefs";
    private static final String SIM_SLOT_KEY = "sim_slot";
    private Spinner simSpinner;
    private List<SubscriptionInfo> subscriptionInfoList;

    private EditText phoneNumberInput;
    private EditText messageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        simSpinner = findViewById(R.id.sim_spinner);
        Button saveSimButton = findViewById(R.id.save_sim_button);

        saveSimButton.setOnClickListener(v -> {
            int selectedSim = simSpinner.getSelectedItemPosition();
            saveSelectedSim(selectedSim);
            Toast.makeText(this, "SIM " + (selectedSim + 1) + " saved.", Toast.LENGTH_SHORT).show();
        });

        Button sendButton = findViewById(R.id.send_button);
        phoneNumberInput = findViewById(R.id.phone_number);
        messageInput = findViewById(R.id.message_text);

        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE},
                    SMS_PERMISSION_CODE);
        } else {
            setupSimSpinner();
            handleIncomingIntent();
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSmsFromSelectedSim();
            }
        });
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        String phone = intent.getStringExtra("phone_number");
        String message = intent.getStringExtra("message");

        if (phone != null && message != null) {
            if (subscriptionInfoList == null || subscriptionInfoList.isEmpty()) {
                Toast.makeText(this, "No active SIM found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use the first SIM by default when triggered from intent extras
            //int subscriptionId = subscriptionInfoList.get(0).getSubscriptionId();

            int savedSimIndex = loadSelectedSim();
            if (savedSimIndex < 0 || savedSimIndex >= subscriptionInfoList.size()) {
                savedSimIndex = 0; // Fallback to SIM 1 if saved index is invalid
            }
            int subscriptionId = subscriptionInfoList.get(savedSimIndex).getSubscriptionId();


            sendSms(phone, message, subscriptionId);

            // Close activity after sending automatically
            finish();
        }
    }

    private void setupSimSpinner() {
        SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission READ_PHONE_STATE required", Toast.LENGTH_SHORT).show();
            return;
        }

        subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionInfoList == null || subscriptionInfoList.isEmpty()) {
            Toast.makeText(this, "No active SIM found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] simNames = new String[subscriptionInfoList.size()];
        for (int i = 0; i < subscriptionInfoList.size(); i++) {
            simNames[i] = subscriptionInfoList.get(i).getCarrierName().toString();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, simNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        simSpinner.setAdapter(adapter);

        // Load and set the saved SIM selection
        int savedSimIndex = loadSelectedSim();
        if (savedSimIndex >= 0 && savedSimIndex < simSpinner.getCount()) {
            simSpinner.setSelection(savedSimIndex);
        } else {
            simSpinner.setSelection(0); // Fallback to default SIM 1 if out of bounds
        }
    }

    private void sendSmsFromSelectedSim() {
        if (subscriptionInfoList == null || subscriptionInfoList.isEmpty()) {
            Toast.makeText(this, "No SIM info available", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = simSpinner.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= subscriptionInfoList.size()) {
            Toast.makeText(this, "Invalid SIM selected", Toast.LENGTH_SHORT).show();
            return;
        }

        //int subscriptionId = subscriptionInfoList.get(selectedPosition).getSubscriptionId();

        int subscriptionId = loadSelectedSim();

        String phoneNumber = phoneNumberInput.getText().toString();
        String message = messageInput.getText().toString();

        if (phoneNumber.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please enter phone number and message", Toast.LENGTH_SHORT).show();
            return;
        }

        sendSms(phoneNumber, message, subscriptionId);
    }

    private void sendSms(String phoneNumber, String message, int subscriptionId) {
        try {
            SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);

            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE);

            smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);

            String simName = "SIM";
            if (subscriptionInfoList != null && !subscriptionInfoList.isEmpty()) {
                for (SubscriptionInfo info : subscriptionInfoList) {
                    if (info.getSubscriptionId() == subscriptionId) {
                        simName = info.getCarrierName().toString();
                        break;
                    }
                }
            }

            Toast.makeText(this, "SMS sent via " + simName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("SmsSender", "SMS send failed", e);
        }
    }

    private void saveSelectedSim(int slot) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SIM_SLOT_KEY, slot);
        editor.apply();
    }

    private int loadSelectedSim() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(SIM_SLOT_KEY, 0);   // Default SIM 1
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                setupSimSpinner();
                handleIncomingIntent();
            } else {
                Toast.makeText(this, "Permissions denied, closing app", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
