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
 * IME 설정 화면.
 * 앱 런처에서 접근 가능하며, 키보드 활성화 안내와 기본 설정을 제공합니다.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 키보드 활성화 버튼
        Button btnEnableKeyboard = findViewById(R.id.btn_enable_keyboard);
        btnEnableKeyboard.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            startActivity(intent);
        });

        // 키보드 선택 버튼
        Button btnSelectKeyboard = findViewById(R.id.btn_select_keyboard);
        btnSelectKeyboard.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
            }
        });

        // 펜 두께 설정
        SeekBar seekBarThickness = findViewById(R.id.seekbar_thickness);
        TextView tvThicknessValue = findViewById(R.id.tv_thickness_value);
        seekBarThickness.setProgress(4); // 기본값 4
        tvThicknessValue.setText("4");
        seekBarThickness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = Math.max(1, progress);
                tvThicknessValue.setText(String.valueOf(value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // 필압 감지 설정
        Switch switchPressure = findViewById(R.id.switch_pressure);
        switchPressure.setChecked(true);
    }
}
