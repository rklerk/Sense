package nl.sense.android.demo.Storage;

import java.util.Date;

/**
 * Created by Ricardo on 16-2-2015.
 */
public class HealthStorage {
    private static int weight;        // gram
    private static int length;        // cm
    private static int bloodPressure; // millimeter of mercury
    private static Date date;
    private static double bmi;

    public HealthStorage() {
        date = new Date();
    }

    public double getBmi() {
        if(weight == 0 || length == 0)
            return 0;

        bmi = ((weight / 1000) / (length/100 * length/100));
        return bmi;
    }

}
