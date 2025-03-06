package com.example.myapplication;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class StepCounterActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private TextView stepCountTextView;
    private int stepCount = 0;

    private KalmanFilter kalmanFilter;
    private static final float STEP_THRESHOLD = 1.2f;  // Динамический порог
    private float lastMagnitude = 0;
    private long lastStepTime = 0;
    private static final int STEP_TIME_DIFF = 300;  // Минимальное время между шагами (мс)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_counter);

        stepCountTextView = findViewById(R.id.step_count_text_view);
        kalmanFilter = new KalmanFilter(0.001f, 0.1f);  // Инициализация фильтра
        stepCountTextView.setText("Steps: " + stepCount);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float rawMagnitude = (float) Math.sqrt(
                    event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]
            );

            float filteredMagnitude = kalmanFilter.update(rawMagnitude);  // Применяем фильтр Калмана
            float delta = Math.abs(filteredMagnitude - lastMagnitude);

            long currentTime = System.currentTimeMillis();
            if (delta > STEP_THRESHOLD && (currentTime - lastStepTime > STEP_TIME_DIFF)) {
                stepCount++;
                lastStepTime = currentTime;
                stepCountTextView.setText("Steps: " + stepCount);
            }

            lastMagnitude = filteredMagnitude;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("stepCount", stepCount);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        stepCount = savedInstanceState.getInt("stepCount");
        stepCountTextView.setText("Steps: " + stepCount);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // Фильтр Калмана для сглаживания шума
    private static class KalmanFilter {
        private float q; // Process noise covariance
        private float r; // Measurement noise covariance
        private float x; // Value
        private float p; // Estimation error covariance
        private float k; // Kalman gain

        public KalmanFilter(float q, float r) {
            this.q = q;
            this.r = r;
            this.p = 1;
            this.x = 0;
        }

        public float update(float measurement) {
            p = p + q;
            k = p / (p + r);
            x = x + k * (measurement - x);
            p = (1 - k) * p;
            return x;
        }
    }
}
