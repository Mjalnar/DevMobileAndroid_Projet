package fr.android.carnetvoyage.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "carnet.db";
    private static final int DATABASE_VERSION = 25; // Augmenté pour forcer onUpgrade

    public static final String TABLE_ENTRIES = "entries";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_NOTE = "note";
    public static final String COLUMN_PHOTO_PATH = "photo_path";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_REMOTE_ID = "remote_id";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_ENTRIES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_NOTE + " TEXT, " +
                    COLUMN_PHOTO_PATH + " TEXT, " +
                    COLUMN_LATITUDE + " REAL, " +
                    COLUMN_LONGITUDE + " REAL, " +
                    COLUMN_ADDRESS + " TEXT, " +
                    COLUMN_TIMESTAMP + " INTEGER, " +
                    COLUMN_REMOTE_ID + " INTEGER DEFAULT -1" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        seedDatabase(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRIES);
        onCreate(db);
    }

    private void seedDatabase(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        // Ajout des lieux automatiques pour Lucas (Personne A)
        addStub(db, "Tour Eiffel", "Vue magnifique au coucher du soleil", 48.8584, 2.2945, "Champ de Mars, 75007 Paris", now - 86400000L);
        addStub(db, "Calanque de Sormiou", "Randonnée puis baignade", 43.2096, 5.4180, "Marseille", now - 3600000L);
        addStub(db, "Mont Saint-Michel", "Marée impressionnante", 48.6361, -1.5115, "Le Mont-Saint-Michel", now);
    }

    private void addStub(SQLiteDatabase db, String title, String note, double lat, double lng, String addr, long ts) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_NOTE, note);
        values.put(COLUMN_PHOTO_PATH, (String) null);
        values.put(COLUMN_LATITUDE, lat);
        values.put(COLUMN_LONGITUDE, lng);
        values.put(COLUMN_ADDRESS, addr);
        values.put(COLUMN_TIMESTAMP, ts);
        values.put(COLUMN_REMOTE_ID, -1L);
        db.insert(TABLE_ENTRIES, null, values);
    }
}
