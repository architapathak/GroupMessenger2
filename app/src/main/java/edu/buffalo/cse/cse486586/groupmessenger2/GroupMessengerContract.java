package edu.buffalo.cse.cse486586.groupmessenger2;

import android.provider.BaseColumns;

/**
 * Created by Archita Pathak on 3/5/2017.
 */

public class GroupMessengerContract {
    //Container class. Inner class defines table and column name.
    private GroupMessengerContract() {
    }

    public static class GroupMessenger implements BaseColumns {
        public static final String TABLE_NAME = "GroupMessages";
        public static final String COLUMN_NAME_KEY = "key";
        public static final String COLUMN_NAME_VALUE = "value";
    }
}
