package com.example.stepappv4;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.anychart.scales.DateTime;

import java.text.SimpleDateFormat;
import java.time.Period;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class StepAppOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "stepapp";
    public static final String TABLE_NAME = "num_steps";
    public static final String KEY_ID = "id";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_DAY = "day";
    public static final String KEY_HOUR = "hour";
    public static final String CREATE_TABLE_SQL = "CREATE TABLE  " + TABLE_NAME + " (" + KEY_ID + " INTEGER PRIMARY KEY, " +
            KEY_DAY + " TEXT, " + KEY_HOUR + "  TEXT, " + KEY_TIMESTAMP + "  TEXT);";

    public StepAppOpenHelper (Context context)
    {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

        // Load all records in the database
    public static void loadRecords(Context context){
        List<String> dates = new LinkedList<String>();
        StepAppOpenHelper databaseHelper = new StepAppOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        String [] columns = new String [] {StepAppOpenHelper.KEY_TIMESTAMP};
        Cursor cursor = database.query(StepAppOpenHelper.TABLE_NAME, columns, null, null, StepAppOpenHelper.KEY_TIMESTAMP,
                null, null );

        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            dates.add(cursor.getString(0));
            cursor.moveToNext();
        }
        database.close();

        Log.d("STORED TIMESTAMPS: ", String.valueOf(dates));
    }

    // load records from a single day
    public static Integer loadSingleRecord(Context context, String date){
        List<String> steps = new LinkedList<String>();
        // Get the readable database
        StepAppOpenHelper databaseHelper = new StepAppOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        String where = StepAppOpenHelper.KEY_DAY + " = ?";
        String [] whereArgs = { date };

        Cursor cursor = database.query(StepAppOpenHelper.TABLE_NAME, null, where, whereArgs, null,
                null, null );

        // iterate over returned elements
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            steps.add(cursor.getString(0));
            cursor.moveToNext();
        }
        database.close();

        Integer numSteps = steps.size();
        Log.d("STORED STEPS TODAY: ", String.valueOf(numSteps));
        return numSteps;
    }

    public static void deleteRecords (Context context) {
        StepAppOpenHelper databaseHelper = new StepAppOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        int numberDeletedRecords = 0;

        numberDeletedRecords = database.delete(StepAppOpenHelper.TABLE_NAME, null, null);
        database.close();

        Toast.makeText(context, "Deleted + "+ String.valueOf(numberDeletedRecords) + " steps", Toast.LENGTH_LONG).show();

    }


    public static void mockDatabaseEntries(Context context, Date date){
        StepAppOpenHelper databaseOpenHelper = new StepAppOpenHelper(context);
        SQLiteDatabase database = databaseOpenHelper.getWritableDatabase();
        for (int i = 0; i < 20; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_MONTH, -i);
            Date tmpDate = cal.getTime();

            String current_time = new SimpleDateFormat("yyyy-MM-dd").format(tmpDate);
            String currentHour = new SimpleDateFormat("HH").format(tmpDate);
            String currentTimeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(tmpDate);

            for (int j = 1; j <20 ; j++) {
                ContentValues databaseEntry = new ContentValues();
                databaseEntry.put(StepAppOpenHelper.KEY_TIMESTAMP, currentTimeStamp);

                databaseEntry.put(StepAppOpenHelper.KEY_DAY, current_time);
                databaseEntry.put(StepAppOpenHelper.KEY_HOUR, currentHour);

                database.insert(StepAppOpenHelper.TABLE_NAME, null, databaseEntry);
            }

        }
    }


    public static Map<String, Integer> loadPreviousSevenDaysSteps(Context context, Date date){
        // 1. Define a map to store the hour and number of steps as key-value pairs
        Map<String, Integer>  dayToStepCount = new TreeMap<>();

        // 2. Get the readable database
        StepAppOpenHelper databaseHelper = new StepAppOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        String current_time = new SimpleDateFormat("yyyy-MM-dd").format(date);

        // 3. Define the query to get the data
        Cursor cursor = database.rawQuery("SELECT day, COUNT(*)  FROM num_steps " +
                "WHERE day <= ? GROUP BY day ORDER BY  day DESC LIMIT 7", new String [] {current_time});

        // 4. Iterate over returned elements on the cursor
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            String tmpKey = cursor.getString(0);
            Integer tmpValue = Integer.parseInt(cursor.getString(1));

            //2. Put the data from the database into the map
            dayToStepCount.put(tmpKey, tmpValue);


            cursor.moveToNext();
        }

        // 5. Close the cursor and database
        cursor.close();
        database.close();

        // 6. Return the map with hours and number of steps
        return correctEntries(dayToStepCount, date);
    }


    private static TreeMap<String, Integer> correctEntries(Map<String, Integer> dayToSteps, Date date){
        TreeMap<String, Integer> correctedDayToSteps = new TreeMap<>();
        Calendar cal = Calendar.getInstance();

        for(int i = 0; i<7; i++){
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_MONTH, -i);
            String currDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
            if(dayToSteps.containsKey(currDate)){
                Integer steps = dayToSteps.get(currDate);
                correctedDayToSteps.put(currDate, steps);
            }else {
                correctedDayToSteps.put(currDate, 0);
            }

        }
        return correctedDayToSteps;
    }



    public static Map<Integer, Integer> loadStepsByHour(Context context, String date){
        // 1. Define a map to store the hour and number of steps as key-value pairs
        Map<Integer, Integer>  map = new HashMap<>();

        // 2. Get the readable database
        StepAppOpenHelper databaseHelper = new StepAppOpenHelper(context);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        // 3. Define the query to get the data
        Cursor cursor = database.rawQuery("SELECT hour, COUNT(*)  FROM num_steps " +
                "WHERE day = ? GROUP BY hour ORDER BY  hour ASC ", new String [] {date});

        // 4. Iterate over returned elements on the cursor
        cursor.moveToFirst();
        for (int index=0; index < cursor.getCount(); index++){
            Integer tmpKey = Integer.parseInt(cursor.getString(0));
            Integer tmpValue = Integer.parseInt(cursor.getString(1));

            //2. Put the data from the database into the map
            map.put(tmpKey, tmpValue);


            cursor.moveToNext();
        }

        // 5. Close the cursor and database
        cursor.close();
        database.close();

        // 6. Return the map with hours and number of steps
        return map;
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL(CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
