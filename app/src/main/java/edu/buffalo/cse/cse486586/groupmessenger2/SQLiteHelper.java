package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Archita Pathak on 3/5/2017.
 */

public class SQLiteHelper extends SQLiteOpenHelper {
    //Method to create the table
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + GroupMessengerContract.GroupMessenger.TABLE_NAME + " (" +
                    GroupMessengerContract.GroupMessenger.COLUMN_NAME_KEY + " TEXT PRIMARY KEY," +
                    GroupMessengerContract.GroupMessenger.COLUMN_NAME_VALUE + " TEXT)";

    //Method to delete the table
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + GroupMessengerContract.GroupMessenger.TABLE_NAME;

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "GroupMessenger.db";

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
