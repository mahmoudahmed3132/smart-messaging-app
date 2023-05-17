package com.cairo.chat.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.cairo.chat.model.Friends;
import com.cairo.chat.model.FriendsList;

public final class FriendDataBase {
    private static FriendDBHelper mDbHelper = null;

    private FriendDataBase() {
    }

    private static FriendDataBase instance = null;

    public static FriendDataBase getInstance(Context context) {
        if (instance == null) {
            instance = new FriendDataBase();
            mDbHelper = new FriendDBHelper(context);
        }
        return instance;
    }

    public long addFriend(Friends friends) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedEntry.COLUMN_NAME_ID, friends.id);
        values.put(FeedEntry.COLUMN_NAME_NAME, friends.name);
        values.put(FeedEntry.COLUMN_NAME_EMAIL, friends.email);
        values.put(FeedEntry.COLUMN_NAME_ID_ROOM, friends.idRoom);
        values.put(FeedEntry.COLUMN_NAME_AVATA, friends.avata);
        return db.insert(FeedEntry.TABLE_NAME, null, values);
    }

    public void addListFriend(FriendsList friendsList){
        for(Friends friends : friendsList.getFriendsList()){
            addFriend(friends);
        }
    }

    public FriendsList getFriendsList() {
        FriendsList friendsList = new FriendsList();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        try {
            Cursor cursor = db.rawQuery("select * from " + FeedEntry.TABLE_NAME, null);
            while (cursor.moveToNext()) {
                Friends friends = new Friends();
                friends.id = cursor.getString(0);
                friends.name = cursor.getString(1);
                friends.email = cursor.getString(2);
                friends.idRoom = cursor.getString(3);
                friends.avata = cursor.getString(4);
                friendsList.getFriendsList().add(friends);
            }
            cursor.close();
        }catch (Exception e){
            return new FriendsList();
        }
        return friendsList;
    }

    public void dropDB(){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.execSQL(SQL_DELETE_ENTRIES);
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public static class FeedEntry implements BaseColumns {
        static final String TABLE_NAME = "friend";
        static final String COLUMN_NAME_ID = "friendID";
        static final String COLUMN_NAME_NAME = "name";
        static final String COLUMN_NAME_EMAIL = "email";
        static final String COLUMN_NAME_ID_ROOM = "idRoom";
        static final String COLUMN_NAME_AVATA = "avatar";
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                    FeedEntry.COLUMN_NAME_ID + " TEXT PRIMARY KEY," +
                    FeedEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                    FeedEntry.COLUMN_NAME_EMAIL + TEXT_TYPE + COMMA_SEP +
                    FeedEntry.COLUMN_NAME_ID_ROOM + TEXT_TYPE + COMMA_SEP +
                    FeedEntry.COLUMN_NAME_AVATA + TEXT_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;


    private static class FriendDBHelper extends SQLiteOpenHelper {
        static final int DATABASE_VERSION = 1;
        static final String DATABASE_NAME = "FriendChat.db";

        FriendDBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}
