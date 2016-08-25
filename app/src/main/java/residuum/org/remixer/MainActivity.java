package residuum.org.remixer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import residuum.org.remixer.widgets.LogSeekBar;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String Tag = "OrgonRemixer";
    private PdUiDispatcher dispatcher;
    private SensorManager sensorManager;

    private Sensor accelerator;
    private boolean acceleratorInitialized;
    private float ax, ay, az;
    private final float shakeThreshold = 10;
    private long lastUpdate;

    private Sensor rotation;
    private boolean rotationInitialized;
    private float rx, ry, rz;
    private final float rotationfactor = 50;

    private LogSeekBar speedbar;
    private LogSeekBar pitchbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final double maxFactor = 3;
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerator = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null) {
            rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        }

        setContentView(R.layout.activity_main);
        try {
            initPd();
            loadPatch();
        } catch (IOException e) {
            Log.e(Tag, e.toString());
            finish();
        }
        speedbar = (LogSeekBar) findViewById(R.id.speedbar);
        speedbar.setRealMinimum(1 / maxFactor);
        speedbar.setRealMaximum(maxFactor);
        speedbar.setLogScale(true);
        speedbar.setRealValue(1);
        final TextView speedvalue = (TextView) findViewById(R.id.speedvalue);
        speedbar.addListener(new LogSeekBar.ChangeListener() {
            @Override
            public void onChange(double newValue) {
                PdBase.sendFloat("speed", (float) newValue);
                speedvalue.setText(String.valueOf(newValue));
            }
        });
        pitchbar = (LogSeekBar) findViewById(R.id.pitchbar);
        pitchbar.setRealMinimum(1 / maxFactor);
        pitchbar.setRealMaximum(maxFactor);
        pitchbar.setLogScale(true);
        pitchbar.setRealValue(1);
        final TextView pitchvalue = (TextView) findViewById(R.id.pitchvalue);
        pitchbar.addListener(new LogSeekBar.ChangeListener() {
            @Override
            public void onChange(double newValue) {
                PdBase.sendFloat("pitch", (float) newValue);
                pitchvalue.setText(String.valueOf(newValue));
            }
        });
        Button sample = (Button) findViewById(R.id.button);
        sample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNewStart();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        PdAudio.startAudio(this);
        getNewStart();
        if (accelerator != null) {
            sensorManager.registerListener(this, accelerator, SensorManager.SENSOR_DELAY_GAME);
        }
        if (rotation != null) {
            sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PdAudio.release();
        PdBase.release();
        sensorManager.unregisterListener(this);
        acceleratorInitialized = false;
        rotationInitialized = false;
    }

    private void getNewStart() {
        lastUpdate = Calendar.getInstance().getTimeInMillis();
        PdBase.sendBang("restart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        PdAudio.stopAudio();
    }

    private void initPd() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        PdAudio.initAudio(sampleRate, 0, 2, 8, true);
        dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(dispatcher);
    }

    private void loadPatch() throws IOException {
        File dir = getFilesDir();
        IoUtils.extractZipResource(getResources().openRawResource(R.raw.remix), dir, true);
        File patch = new File(dir, "remix.pd");
        PdBase.openPatch(patch.getAbsolutePath());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                updateSpeedAndPitch(event);
                testForShaking(event);
                break;
        }
    }

    private void updateSpeedAndPitch(SensorEvent event) {
        float changeX = event.values[0] - rx;
        float changeY = event.values[1] - ry;
        float changeZ = event.values[2] - rz;
        rx = event.values[0];
        ry = event.values[1];
        rz = event.values[2];
        if (!rotationInitialized) {
            rotationInitialized = true;
            return;
        }
        speedbar.setProgress(speedbar.getProgress() + (int) (rotationfactor * changeZ));
        pitchbar.setProgress(pitchbar.getProgress() + (int) (rotationfactor * changeY));
    }

    private void testForShaking(SensorEvent event) {
        float changeX = Math.abs(event.values[0] - ax);
        float changeY = Math.abs(event.values[1] - ay);
        float changeZ = Math.abs(event.values[2] - az);
        ax = event.values[0];
        ay = event.values[1];
        az = event.values[2];
        if (!acceleratorInitialized) {
            acceleratorInitialized = true;
            return;
        }
        if (changeX < shakeThreshold && changeY < shakeThreshold && changeZ < shakeThreshold) {
            return;
        }
        long now = Calendar.getInstance().getTimeInMillis();
        if (now - lastUpdate < 1000) {
            return;
        }
        getNewStart();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
