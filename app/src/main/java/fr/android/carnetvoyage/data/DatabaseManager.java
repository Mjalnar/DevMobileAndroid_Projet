package fr.android.carnetvoyage.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.android.carnetvoyage.model.Entry;

public class DatabaseManager extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "carnet.db";
    private static final int DATABASE_VERSION = 27;

    public static final String TABLE_ENTRIES = "entries";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_NOTE = "note";
    public static final String COL_PHOTO = "photo_path";
    public static final String COL_LAT = "latitude";
    public static final String COL_LNG = "longitude";
    public static final String COL_ADDR = "address";
    public static final String COL_TIME = "timestamp";
    public static final String COL_REMOTE = "remote_id";

    public DatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ENTRIES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_NOTE + " TEXT, " +
                COL_PHOTO + " TEXT, " +
                COL_LAT + " REAL, " +
                COL_LNG + " REAL, " +
                COL_ADDR + " TEXT, " +
                COL_TIME + " INTEGER, " +
                COL_REMOTE + " INTEGER DEFAULT -1);");

        db.execSQL("INSERT INTO " + TABLE_ENTRIES + " (" +
                COL_TITLE + ", " + COL_NOTE + ", " + COL_LAT + ", " + COL_LNG + ", " + COL_ADDR + ", " + COL_TIME +
                ") SELECT 'Tour Eiffel', 'Un lieu magnifique à visiter !', 48.8584, 2.2945, 'Champ de Mars, 5 Av. Anatole France, 75007 Paris', " + System.currentTimeMillis() +
                " WHERE NOT EXISTS (SELECT 1 FROM " + TABLE_ENTRIES + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public long addEntry(Entry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_TITLE, entry.getTitle());
        v.put(COL_NOTE, entry.getNote());
        v.put(COL_PHOTO, entry.getPhotoPath());
        v.put(COL_LAT, entry.getLatitude());
        v.put(COL_LNG, entry.getLongitude());
        v.put(COL_ADDR, entry.getAddress());
        v.put(COL_TIME, entry.getTimestamp());
        v.put(COL_REMOTE, entry.getRemoteId());
        return db.insert(TABLE_ENTRIES, null, v);
    }

    public List<Entry> getAllEntries() {
        List<Entry> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_ENTRIES, null, null, null, null, null, COL_TIME + " DESC");

        if (c.moveToFirst()) {
            do {
                list.add(cursorToEntry(c));
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public Entry getById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_ENTRIES, null, COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);

        Entry entry = null;
        if (c.moveToFirst()) {
            entry = cursorToEntry(c);
        }
        c.close();
        return entry;
    }

    public Set<Long> getKnownRemoteIds() {
        Set<Long> ids = new HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_ENTRIES, new String[]{COL_REMOTE}, null, null, null, null, null);
        if (c.moveToFirst()) {
            do {
                ids.add(c.getLong(0));
            } while (c.moveToNext());
        }
        c.close();
        return ids;
    }

    public void deleteEntry(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ENTRIES, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void updateRemoteId(long localId, long remoteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_REMOTE, remoteId);
        db.update(TABLE_ENTRIES, v, COL_ID + "=?", new String[]{String.valueOf(localId)});
    }

    private Entry cursorToEntry(Cursor c) {
        Entry e = new Entry();
        e.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        e.setTitle(c.getString(c.getColumnIndexOrThrow(COL_TITLE)));
        e.setNote(c.getString(c.getColumnIndexOrThrow(COL_NOTE)));
        e.setPhotoPath(c.getString(c.getColumnIndexOrThrow(COL_PHOTO)));
        e.setLatitude(c.getDouble(c.getColumnIndexOrThrow(COL_LAT)));
        e.setLongitude(c.getDouble(c.getColumnIndexOrThrow(COL_LNG)));
        e.setAddress(c.getString(c.getColumnIndexOrThrow(COL_ADDR)));
        e.setTimestamp(c.getLong(c.getColumnIndexOrThrow(COL_TIME)));
        e.setRemoteId(c.getLong(c.getColumnIndexOrThrow(COL_REMOTE)));
        return e;
    }
}
