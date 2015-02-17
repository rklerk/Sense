package nl.sense.android.demo.Controllers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.sense_os.platform.SenseApplication;
import nl.sense_os.service.SenseServiceStub;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;

public class SenseController extends AsyncTask<SenseApplication, Void, Void> {
    private SenseApplication mApplication;
    private static final String DEMO_SENSOR_NAME = "GT-I9300";
    public static final String TAG = "SenseController";

    public SenseController(SenseApplication application) {
        mApplication = application;
    }

    public void startSense() {
        SenseServiceStub senseService = mApplication.getSenseService();

        // enable some specific sensor modules
        senseService.togglePhoneState(true);
        senseService.toggleAmbience(true);
        senseService.toggleMotion(true);
        senseService.toggleLocation(true);

        // enable main state
        senseService.toggleMain(true);
    }

    public void stopSense() {
        mApplication.getSenseService().toggleMain(false);
    }

    public void flushData() {
        mApplication.getSensePlatform().flushData();
    }

    @Override
    protected Void doInBackground(SenseApplication... params) {
        startSense();
        setPreferences();
        //getAllSensors();
        return null;
    }

    private JSONArray getAllSensors() {
        //String data = SenseApi.getDefaultDeviceType(mApplication.getBaseContext());
        //System.out.println(data);

        JSONArray data;

        try {
           data = SenseApi.getAllSensors(mApplication.getBaseContext());
           return data;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * An example of how to upload data for a custom sensor.
     */
    private void insertData() {
        Log.v(TAG, "Insert data point");

        // Description of the sensor
        final String name = DEMO_SENSOR_NAME;
        final String displayName = "demo data";
        final String dataType = SenseDataTypes.JSON;
        final String description = name;
        // the value to be sent, in json format
        final String value = "{\"foo\":\"bar\",\"baz\":\"quux\"}";
        final long timestamp = System.currentTimeMillis();

        // start new Thread to prevent NetworkOnMainThreadException
        new Thread() {

            @Override
            public void run() {
                mApplication.getSensePlatform().addDataPoint(name, displayName, description,
                        dataType, value, timestamp);
            }
        }.start();
    }

    private void registerSensors(Context context, String sensorName, String displayName, String description,
                                String dataType, String value, String deviceType, String deviceUuid) {

        SensorManager sm = (SensorManager) mApplication.getBaseContext().getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> allSensors = sm.getSensorList(Sensor.TYPE_ALL);

        JSONArray registeredSensors = getAllSensors();

        try {
            for(int i = 0; i < allSensors.size(); i++) {
                for(int j = 0; j < allSensors.size(); j++) {
                        if(registeredSensors.getJSONObject(j).getString("name") == allSensors.get(j).getName()) {
                            // Don't register if the sensor is already registered.
                            continue;
                        }

                    Sensor sensor = allSensors.get(i);
                    SenseApi.registerSensor(mApplication.getApplicationContext(), sensor.getName(), sensor.getName(), "", SenseDataTypes.INT , Float.toString(sensor.getMaximumRange()), null, null);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getLocalData() {

        JSONArray data;
        try {
            data = mApplication.getSensePlatform().getLocalData("acceleration", 100);
            Log.d(TAG, data.toString());
        } catch (IllegalStateException e) {
            Log.w(TAG, "Failed to query remote data", e);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse remote data", e);
        }
    }

    private void registerSensor() {

    }

    private void getRemoteData() {
        Log.v(TAG, "Get data from CommonSense");
        long startDate = new Date().getTime();
        long endDate = new Date().getTime();

        JSONArray data;
        try {
            data = mApplication.getSensePlatform().getData(DEMO_SENSOR_NAME, true, 10, startDate, endDate);

        } catch (IllegalStateException e) {
            Log.w(TAG, "Failed to query remote data", e);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse remote data", e);
        }
    }

    /**
     * Sets up the Sense service preferences
     */
    private void setPreferences() {
        Log.v(TAG, "Set preferences");

        SenseServiceStub senseService = mApplication.getSenseService();

        // turn off some specific sensors

        senseService.setPrefBool(SensePrefs.Main.Ambience.PRESSURE, false);

        // turn on specific sensors
        senseService.setPrefBool(SensePrefs.Main.Ambience.LIGHT, true);
        senseService.setPrefBool(SensePrefs.Main.Ambience.CAMERA_LIGHT, true);
        senseService.setPrefBool(SensePrefs.Main.Ambience.MIC, true);
        // NOTE: spectrum might be too heavy for the phone or consume too much energy
        senseService.setPrefBool(SensePrefs.Main.Ambience.AUDIO_SPECTRUM, true);

        // set how often to sample
        // 1 := rarely (~every 15 min)
        // 0 := normal (~every 5 min)
        // -1 := often (~every 10 sec)
        // -2 := real time (this setting affects power consumption considerably!)
        senseService.setPrefString(SensePrefs.Main.SAMPLE_RATE, "-1");

        // set how often to upload
        // 1 := eco mode (buffer data for 30 minutes before bulk uploading)
        // 0 := normal (buffer 5 min)
        // -1 := often (buffer 1 min)
        // -2 := real time (every new data point is uploaded immediately)
        senseService.setPrefString(SensePrefs.Main.SYNC_RATE, "-2");
    }
}
