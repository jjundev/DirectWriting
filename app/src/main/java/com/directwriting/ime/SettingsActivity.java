package com.directwriting.ime;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * IME settings screen available from launcher.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button btnEnableKeyboard = findViewById(R.id.btn_enable_keyboard);
        btnEnableKeyboard.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            startActivity(intent);
        });

        Button btnSelectKeyboard = findViewById(R.id.btn_select_keyboard);
        btnSelectKeyboard.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
            }
        });

        SeekBar seekBarThickness = findViewById(R.id.seekbar_thickness);
        TextView tvThicknessValue = findViewById(R.id.tv_thickness_value);

        int savedThickness = clampThickness(ImePreferences.getPenThickness(this), seekBarThickness.getMax());
        seekBarThickness.setProgress(savedThickness);
        tvThicknessValue.setText(String.valueOf(savedThickness));

        seekBarThickness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = clampThickness(progress, seekBar.getMax());
                if (progress != value) {
                    seekBar.setProgress(value);
                    return;
                }

                tvThicknessValue.setText(String.valueOf(value));
                ImePreferences.setPenThickness(SettingsActivity.this, value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Switch switchPressure = findViewById(R.id.switch_pressure);
        switchPressure.setChecked(ImePreferences.isPressureSensitivityEnabled(this));
        switchPressure.setOnCheckedChangeListener((buttonView, isChecked) ->
                ImePreferences.setPressureSensitivityEnabled(SettingsActivity.this, isChecked));
    }

    private int clampThickness(int value, int max) {
        return Math.max(1, Math.min(value, max));
    }
}
