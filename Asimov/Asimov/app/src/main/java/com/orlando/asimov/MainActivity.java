package com.orlando.asimov;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.orlando.arduino.Arduino;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import java.util.Set;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    TextView mLockStateView;
    TextView mRoll;
    TextView mPitch;
    TextView mYaw;
    Arduino arduino;
    TextView mArm;
    TextView mAndroidRoll;
    TextView mAndroidPitch;
    TextView mAndroidYaw;
    ImageView mGesture;
    TextView mRightArm;
    TextView mLegs;
    boolean fist = false;
    private long lastUpdate = 0;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private float last_x, last_y, last_z;
    double fx = 0;
    double fy = 0;
    double fz = 0;
    double roll, pitch, yaw;
    private static final int SHAKE_THRESHOLD = 600;
    private static final float ALPHA = 0.5f;
    NeuralNetwork rightArmNet;
    String spread = String.valueOf((char)91);
    String forward = String.valueOf((char)92);
    String backward = String.valueOf((char)93);
    String left = String.valueOf((char)94);
    String right = String.valueOf((char)95);
    String stop = String.valueOf((char)96);
    double[][] armW1 = {
            {-2.618367, 0.1152786, 1.0609498, -0.1182739, 1.4148466},
            {-1.8480779, -0.5230582, 0.6904515, -0.0686407, 1.1100996},
            {1.7063321, 0.411104, -0.4178046, -0.3111318, -0.4470024}};
    double[][] armW2 = {
            {-4.0752674}, {-0.3026071}, {1.2239282}, {-0.2081355}, {1.9520936}
    };
    private DeviceListener mListener = new AbstractDeviceListener() {
        // onConnect() is called whenever a Myo has been connected.
        @Override
        public void onConnect(Myo myo, long timestamp) {
            // Set the text color of the text view to cyan when a Myo connects.
        }
        // onDisconnect() is called whenever a Myo has been disconnected.
        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            // Set the text color of the text view to red when a Myo disconnects.
        }
        // onArmSync() is called whenever Myo has recognized a Sync Gesture after someone has put it on their
        // arm. This lets Myo know which arm it's on and which way it's facing.
        @Override
        public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            mArm.setText(myo.getArm() == Arm.LEFT ? R.string.arm_left : R.string.arm_right);
            arduino.connect();
        }
        // onArmUnsync() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
        // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
        // when Myo is moved around on the arm.
        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            arduino.disconnect();
        }
        // onUnlock() is called whenever a synced Myo has been unlocked. Under the standard locking
        // policy, that means poses will now be delivered to the listener.
        @Override
        public void onUnlock(Myo myo, long timestamp) {
            mLockStateView.setText(R.string.unlocked);
        }
        // onLock() is called whenever a synced Myo has been locked. Under the standard locking
        // policy, that means poses will no longer be delivered to the listener.
        @Override
        public void onLock(Myo myo, long timestamp) {
            mLockStateView.setText(R.string.locked);
        }
        // onOrientationData() is called whenever a Myo provides its current orientation,
        // represented as a quaternion.
        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
            float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));
            // Adjust roll and pitch for the orientation of the Myo on the arm.
            if (myo.getXDirection() == XDirection.TOWARD_ELBOW) {
                roll *= -1;
                pitch *= -1;
            }

            //Se mapean los angulos de 0 a 180
            roll = map((int)roll, -180, 180, 180, 0);
            pitch = map((int)pitch, -180, 180, 0, 180);;
            yaw = map((int)yaw, -180, 180, 0, 180);

            // Next, we apply a rotation to the text view using the roll, pitch, and yaw.
            mRoll.setText(String.format("Roll: %.0f", roll));
            mPitch.setText(String.format("Pitch: %.0f", pitch));
            mYaw.setText(String.format("Yaw: %.0f", yaw));

            if(fist){
                double[][] rArm = rightArmNet.forward(roll, pitch);
                int valRArm = (int)rArm[0][0];
                mRightArm.setText(String.format("Right arm value: %d", valRArm));
                char armASCCI = (char) valRArm;
                arduino.write(String.valueOf(armASCCI));
            }



        }
        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            switch (pose) {
                case UNKNOWN:
                    fist = false;
                    break;
                case REST:
                    fist = false;
                    break;
                case DOUBLE_TAP:
                    fist = true;
                    mGesture.setImageDrawable(getDrawable(R.drawable.double_tap));
                    //arduino.write("t");
                    break;
                case FIST:
                    fist = true;
                    mGesture.setImageDrawable(getDrawable(R.drawable.fist));
                    //arduino.write("f");
                    break;
                case WAVE_IN:
                    fist = true;
                    mGesture.setImageDrawable(getDrawable(R.drawable.wave_in));
                    //arduino.write("w");
                    break;
                case WAVE_OUT:
                    fist = false;
                    arduino.write(spread);
                    mGesture.setImageDrawable(getDrawable(R.drawable.wave_out));
                    //arduino.write("W");
                    break;
                case FINGERS_SPREAD:
                    fist = false;
                    arduino.write(spread);
                    mGesture.setImageDrawable(getDrawable(R.drawable.spread_fingers));
                    //arduino.write("s");
                    break;
            }
            if (pose != Pose.UNKNOWN && pose != Pose.REST) {
                // Tell the Myo to stay unlocked until told otherwise. We do that here so you can
                // hold the poses without the Myo becoming locked.
                myo.unlock(Myo.UnlockType.HOLD);
                // Notify the Myo that the pose has resulted in an action, in this case changing
                // the text on the screen. The Myo will vibrate.
                myo.notifyUserAction();
            } else {
                // Tell the Myo to stay unlocked only for a short period. This allows the Myo to
                // stay unlocked while poses are being performed, but lock after inactivity.
                myo.unlock(Myo.UnlockType.HOLD);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        rightArmNet = new NeuralNetwork();
        rightArmNet.setW1(armW1);
        rightArmNet.setW2(armW2);
        arduino = new Arduino(this);
        mGesture = (ImageView) findViewById(R.id.gesture_img);
        mLockStateView = (TextView) findViewById(R.id.state_lbl);
        mRoll = (TextView) findViewById(R.id.roll_lbl);
        mPitch = (TextView) findViewById(R.id.pitch_lbl);
        mYaw = (TextView) findViewById(R.id.yaw_lbl);
        mArm = (TextView)findViewById(R.id.arm_lbl);
        mAndroidRoll = (TextView)findViewById(R.id.android_roll_lbl);
        mAndroidPitch = (TextView)findViewById(R.id.android_pitch_lbl);
        mAndroidYaw = (TextView)findViewById(R.id.android_yaw_lbl);
        mRightArm = (TextView)findViewById(R.id.rightArm_lbl);
        mLegs = (TextView)findViewById(R.id.legs_lbl);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Conectando a arduino", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                arduino.connect();
            }
        });
        Hub hub = Hub.getInstance();
        if (!hub.init(this)) {
            Toast.makeText(getApplicationContext(), "No se pudo inicializar", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // We don't want any callbacks when the Activity is gone, so unregister the listener.
        Hub.getInstance().removeListener(mListener);
        if (isFinishing()) {
            // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
            Hub.getInstance().shutdown();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            onScanActionSelected();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onScanActionSelected() {
        // Launch the ScanActivity to scan for Myos to connect to.
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }

    int map(int x, int in_min, int in_max, int out_min, int out_max) {
        try{
            return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
        }catch(ArithmeticException e){
            return 0;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        //Configuracion y uso del API del acelerometro
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {

                }

                //se definen last_x, last_y y last_z como los valores finales del acelerometro
                last_x = x;
                last_y = y;
                last_z = z;

                //se limpian un poco los valores fx, fy, fz para mayos estabilidad
                fx = last_x * ALPHA + (fx * (1.0 - ALPHA));
                fy = last_y * ALPHA + (fy * (1.0 - ALPHA));
                fz = last_z * ALPHA + (fz * (1.0 - ALPHA));

                // Se calculan los angulos roll y pitch a partir de los valores del acelerometro
                roll = (Math.atan2(-fy, fz) * 180.0) / Math.PI;
                pitch = (Math.atan2(fx, Math.sqrt(fy * fy + fz * fz)) * 180.0) / Math.PI;
                yaw = 180 * Math.atan2(fz,Math.sqrt(fx*fx + fz*fz))/Math.PI;

                //Se mapean los angulos de 0 a 180
                roll = map((int) roll, -180, 180, 180, 0);
                pitch = map((int) pitch, -180, 180, 0, 180);
                yaw = map((int) yaw, -180, 180, 0, 180);

                mAndroidRoll.setText(String.format("Roll: %.0f", roll));
                mAndroidPitch.setText(String.format("Pitch: %.0f", pitch));
                mAndroidYaw.setText(String.format("Yaw: %.0f", yaw));

                int rollA = (int)roll;

                int pitchA = (int)pitch;

                if(!fist){
                    if(rollA > 100){
                        mLegs.setText("forward");
                        arduino.write(forward);
                    }else if(rollA < 80){
                        mLegs.setText("Backward");
                        arduino.write(backward);
                    }

                    if(pitchA > 100){
                        mLegs.setText("Left");
                        arduino.write(left);
                    }else if(pitchA < 80) {
                        mLegs.setText("Right");
                        arduino.write(right);
                    }

                    if(rollA > 80 && rollA < 100 && pitchA > 80 && pitchA < 100){
                        mLegs.setText("stand");
                        arduino.write(stop);
                    }
                }else{
                    mLegs.setText("stand");
                    arduino.write(stop);
                }

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
