package com.example.bleappv3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Animations extends AppCompatActivity {
    private static final String TAG = "Animations";

    Button offButton, twinkleButton, jinxButton, transitionButton, bluebButton, rainbowButton, beatrButton, runredButton, rawNoiseButton, movingRainbowButton, waveButton, blightbButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.animations);

        offButton = findViewById(R.id.offBtn);
        twinkleButton = findViewById(R.id.twinkleBtn);
        jinxButton = findViewById(R.id.jinxBtn);
        transitionButton = findViewById(R.id.transitionBreakBtn);
        bluebButton = findViewById(R.id.bluebBtn);
        rainbowButton = findViewById(R.id.rainbowBtn);
        beatrButton = findViewById(R.id.beatrBtn);
        runredButton = findViewById(R.id.runredBtn);
        rawNoiseButton = findViewById(R.id.rawNoiseBtn);
        movingRainbowButton = findViewById(R.id.movingRainbowBtn);
        waveButton = findViewById(R.id.waveBtn);
        blightbButton = findViewById(R.id.blightbBtn);

        offButton.setOnClickListener(v -> sendCommand(0));
        twinkleButton.setOnClickListener(v -> sendCommand(1));
        jinxButton.setOnClickListener(v -> sendCommand(2));
        transitionButton.setOnClickListener(v -> sendCommand(3));
        bluebButton.setOnClickListener(v -> sendCommand(4));
        rainbowButton.setOnClickListener(v -> sendCommand(5));
        beatrButton.setOnClickListener(v -> sendCommand(6));
        runredButton.setOnClickListener(v -> sendCommand(7));
        rawNoiseButton.setOnClickListener(v -> sendCommand(8));
        movingRainbowButton.setOnClickListener(v -> sendCommand(9));
        waveButton.setOnClickListener(v -> sendCommand(10));
        blightbButton.setOnClickListener(v -> sendCommand(11));


    }

    private void sendCommand(int command) {
        Intent intent = new Intent(this, BLEService.class);
        intent.putExtra("command", command);
        startService(intent); // Send command via BLE service
    }
}
