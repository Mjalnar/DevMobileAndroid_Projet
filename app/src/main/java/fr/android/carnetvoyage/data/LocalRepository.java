package fr.android.carnetvoyage.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import fr.android.carnetvoyage.model.Entry;

public class LocalRepository implements Repository {

    private final DatabaseHelper dbHelper;

    public LocalRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    @Override
    public long add(Entry entry) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TITLE, entry.getTitle());
        values.put(DatabaseHelper.COLUMN_NOTE, entry.getNote());
        values.put(DatabaseHelper.COLUMN_PHOTO_PATH, entry.getPhotoPath());
        values.put(DatabaseHelper.COLUMN_LATITUDE, entry.getLatitude());
        values.put(DatabaseHelper.COLUMN_LONGITUDE, entry.getLongitude());
        values.put(DatabaseHelper.COLUMN_ADDRESS, entry.getAddress());
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, entry.getTimestamp());
        values.put(DatabaseHelper.COLUMN_REMOTE_ID, entry.getRemoteId());

        long id = db.insert(DatabaseHelper.TABLE_ENTRIES, null, values);
        entry.setId(id);
        return id;
    }

    @Override
    public List<Entry> getAll() {
        List<Entry> entries = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(DatabaseHelper.TABLE_ENTRIES, null, null, null, null, null, DatabaseHelper.COLUMN_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                entries.add(cursorToEntry(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return entries;
    }

    // Utilisé par SyncManager pour réécrire le remoteId après synchronisation.
    public boolean update(Entry entry) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TITLE, entry.getTitle());
        values.put(DatabaseHelper.COLUMN_NOTE, entry.getNote());
        values.put(DatabaseHelper.COLUMN_PHOTO_PATH, entry.getPhotoPath());
        values.put(DatabaseHelper.COLUMN_LATITUDE, entry.getLatitude());
        values.put(DatabaseHelper.COLUMN_LONGITUDE, entry.getLongitude());
        values.put(DatabaseHelper.COLUMN_ADDRESS, entry.getAddress());
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, entry.getTimestamp());
        values.put(DatabaseHelper.COLUMN_REMOTE_ID, entry.getRemoteId());

        int rows = db.update(DatabaseHelper.TABLE_ENTRIES, values, DatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(entry.getId())});
        return rows > 0;
    }

    private Entry cursorToEntry(Cursor cursor) {
        Entry entry = new Entry();
        entry.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
        entry.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE)));
        entry.setNote(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTE)));
        entry.setPhotoPath(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHOTO_PATH)));
        entry.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LATITUDE)));
        entry.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LONGITUDE)));
        entry.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS)));
        entry.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)));
        entry.setRemoteId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REMOTE_ID)));
        return entry;
    }
}
