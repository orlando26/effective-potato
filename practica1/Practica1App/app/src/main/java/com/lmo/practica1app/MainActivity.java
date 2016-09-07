package com.lmo.practica1app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    Button Connect;
    TextView Result;
    TextView pulsillo;
    TextView tempe;
    TextView rollgg;
    TextView pitchgg;
    private String dataToSend;

    private static final String TAG = "Jon";
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    BluetoothDevice device = null;
    private static String address = "XX:XX:XX:XX:XX:XX";
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private InputStream inStream = null;
    Handler handler = new Handler();
    byte delimiter = 10;
    boolean stopWorker = false;
    int readBufferPosition = 0;
    byte[] readBuffer = new byte[1024];
    private long lastUpdate = 0;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private float last_x, last_y, last_z;
    double fx = 0;
    double fy = 0;
    double fz = 0;
    double roll, pitch;
    private static final int SHAKE_THRESHOLD = 600;
    private static final float ALPHA = 0.5f;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Connect = (Button) findViewById(R.id.connect);
        pulsillo=(TextView) findViewById(R.id.Pulsillo);
        Result = (TextView) findViewById(R.id.SerialText);
        tempe = (TextView) findViewById(R.id.Tempe);
        rollgg = (TextView) findViewById(R.id.rollgg);
        pitchgg = (TextView) findViewById(R.id.pitchgg);


        Connect.setOnClickListener(this);
        CheckBt();// = mBluetoothAdapter.getRemoteDevice(address);
        //Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        /*for(BluetoothDevice dev: pairedDevices){
            if(dev.getName() == "HC-05"){
                device = dev;
                break;
            }
        }*/
        device = mBluetoothAdapter.getRemoteDevice("20:16:04:18:43:85");
        //Log.e("Jon", device.toString());
    }

    /**
     * Metodo que se manda a llamar cada vez que el acelerometro detecta movimiento
     * @param sensorEvent
     */
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
                roll  = (Math.atan2(-fy, fz)*180.0)/Math.PI;
                pitch = (Math.atan2(fx, Math.sqrt(fy * fy + fz * fz))*180.0)/Math.PI;

                //Se mapean los angulos de 0 a 180
                roll = map((int)roll, -180, 180, 180, 0);
                pitch = map((int)pitch, -180, 180, 0, 180);

                if(connected){
                    if(roll >= 115 && roll <= 145){
                        writeData("1");
                    }else if((roll >= 80 && roll < 115) | (roll >= 160 && roll <= 180) | (roll >= 1 && roll <= 15)){
                        writeData("0");
                    }
                }

                rollgg.setText(String.valueOf((int)roll));
                pitchgg.setText(String.valueOf((int)pitch));


            }
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {


    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void CheckBt() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth Disabled !",
                    Toast.LENGTH_SHORT).show();
        }

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),
                    "Bluetooth null !", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void connect() {
        connected = true;
        Log.d(TAG, "20:16:04:18:43:85");
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("20:16:04:18:43:85");
        Log.d(TAG, "Connecting to ... " + device);
        //mBluetoothAdapter.cancelDiscovery();
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            btSocket.connect();
            Log.d(TAG, "Connection made.");
        } catch (IOException | NullPointerException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.d(TAG, "Unable to end the connection");
            }
            Log.d(TAG, "Socket creation failed");
        }

        beginListenForData();
    }

    private void writeData(String data) {
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Log.d(TAG, "Bug BEFORE Sending stuff", e);
        }

        String message = data;
        byte[] msgBuffer = message.getBytes();

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            Log.d(TAG, "Bug while sending stuff", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            btSocket.close();
        } catch (IOException e) {
        }
    }

    public void beginListenForData()   {
        try {
            inStream = btSocket.getInputStream();
        } catch (IOException e) {
        }

        Thread workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = inStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {

                                            Result.setText(data);
                                            String[] ambos;
                                            ambos=data.split("-");

                                            if (ambos[0].replaceAll("\\s+","").equals("1")){
                                                pulsillo.setBackgroundColor(Color.parseColor("#ff0000"));
                                            } else{
                                                pulsillo.setBackgroundColor(Color.parseColor("#000000"));
                                            }

                                            tempe.setText(ambos[1]);

                                                        /* You also can use Result.setText(data); it won't display multilines
                                                        */

                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    int map(int x, int in_min, int in_max, int out_min, int out_max) {
        try{
            return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
        }catch(ArithmeticException e){
            return 0;
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        Toast.makeText(this, "no mames " + device.getAddress(), Toast.LENGTH_SHORT).show();
        connect();
    }
}
