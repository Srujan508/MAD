package com.example.miniproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper manages the local SQLite database for storing categorized SMS records.
 * Ensures offline persistence, fast searching, and eliminates duplicate imports.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sms_categorizer.db";
    private static final int DATABASE_VERSION = 1;

    // Table & Columns
    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SMS_ID = "sms_id";
    public static final String COLUMN_SENDER = "sender";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_DATE_MILLIS = "date_millis";
    public static final String COLUMN_FORMATTED_DATE = "formatted_date";
    public static final String COLUMN_CATEGORY = "category";

    // SQL Create Table Statement
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_MESSAGES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_SMS_ID + " TEXT UNIQUE, " +
                    COLUMN_SENDER + " TEXT, " +
                    COLUMN_BODY + " TEXT, " +
                    COLUMN_DATE_MILLIS + " INTEGER, " +
                    COLUMN_FORMATTED_DATE + " TEXT, " +
                    COLUMN_CATEGORY + " TEXT" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    /**
     * Inserts a message, or updates category if the message already exists.
     * Prevents duplicate entries using the unique sms_id column.
     *
     * @param message Message object to insert
     * @return true if inserted or updated successfully
     */
    public synchronized boolean insertOrUpdateMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SMS_ID, message.getSmsId());
        values.put(COLUMN_SENDER, message.getSender());
        values.put(COLUMN_BODY, message.getBody());
        values.put(COLUMN_DATE_MILLIS, message.getDateMillis());
        values.put(COLUMN_FORMATTED_DATE, message.getFormattedDate());
        values.put(COLUMN_CATEGORY, message.getCategory());

        // CONFLICT_REPLACE ensures if sms_id already exists, row is updated cleanly
        long result = db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    /**
     * Retrieves all messages ordered by received date descending (newest first).
     */
    public List<Message> getAllMessages() {
        List<Message> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_MESSAGES,
                null,
                null,
                null,
                null,
                null,
                COLUMN_DATE_MILLIS + " DESC"
        )) {
            while (cursor.moveToNext()) {
                list.add(cursorToMessage(cursor));
            }
        }
        return list;
    }

    /**
     * Retrieves messages for a specific category.
     */
    public List<Message> getMessagesByCategory(String category) {
        List<Message> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_MESSAGES,
                null,
                COLUMN_CATEGORY + " = ?",
                new String[]{category},
                null,
                null,
                COLUMN_DATE_MILLIS + " DESC"
        )) {
            while (cursor.moveToNext()) {
                list.add(cursorToMessage(cursor));
            }
        }
        return list;
    }

    /**
     * Searches messages across sender name, phone number, and message body.
     */
    public List<Message> searchMessages(String query) {
        List<Message> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String wildQuery = "%" + query + "%";
        try (Cursor cursor = db.query(
                TABLE_MESSAGES,
                null,
                COLUMN_SENDER + " LIKE ? OR " + COLUMN_BODY + " LIKE ?",
                new String[]{wildQuery, wildQuery},
                null,
                null,
                COLUMN_DATE_MILLIS + " DESC"
        )) {
            while (cursor.moveToNext()) {
                list.add(cursorToMessage(cursor));
            }
        }
        return list;
    }

    /**
     * Searches messages within a specific category.
     */
    public List<Message> searchMessagesByCategory(String category, String query) {
        List<Message> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String wildQuery = "%" + query + "%";
        try (Cursor cursor = db.query(
                TABLE_MESSAGES,
                null,
                COLUMN_CATEGORY + " = ? AND (" + COLUMN_SENDER + " LIKE ? OR " + COLUMN_BODY + " LIKE ?)",
                new String[]{category, wildQuery, wildQuery},
                null,
                null,
                COLUMN_DATE_MILLIS + " DESC"
        )) {
            while (cursor.moveToNext()) {
                list.add(cursorToMessage(cursor));
            }
        }
        return list;
    }

    /**
     * Returns total count of all messages stored.
     */
    public int getTotalCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MESSAGES, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    /**
     * Returns count of messages in a specific category.
     */
    public int getCategoryCount(String category) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_CATEGORY + " = ?",
                new String[]{category}
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    /**
     * Deletes a single message record by its SQLite id.
     * Note: Does NOT modify or delete original phone SMS!
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean deleteMessage(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_MESSAGES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    /**
     * Clears all local records from SQLite.
     */
    @SuppressWarnings("unused")
    public void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, null, null);
    }

    /**
     * Helper to map a Cursor row to a Message object.
     */
    private Message cursorToMessage(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String smsId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SMS_ID));
        String sender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER));
        String body = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BODY));
        long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE_MILLIS));
        String formattedDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FORMATTED_DATE));
        String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));

        return new Message(id, smsId, sender, body, dateMillis, formattedDate, category);
    }
}
