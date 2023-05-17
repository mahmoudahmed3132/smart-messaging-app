package com.cairo.chat.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.cairo.chat.model.Groups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GroupDataBase {
    private static GroupDataBase.GroupDBHelper mDbHelper = null;

    private GroupDataBase() {
    }

    private static GroupDataBase instance = null;

    public static GroupDataBase getInstance(Context context) {
        if (instance == null) {
            instance = new GroupDataBase();
            mDbHelper = new GroupDataBase.GroupDBHelper(context);
        }
        return instance;
    }

    public void addGroup(Groups groups) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedEntry.COLUMN_GROUP_ID, groups.id);
        values.put(FeedEntry.COLUMN_GROUP_NAME, groups.groupInfo.get("name"));
        values.put(FeedEntry.COLUMN_GROUP_ADMIN, groups.groupInfo.get("admin"));

        for (String idMenber : groups.member) {
            values.put(FeedEntry.COLUMN_GROUP_MEMBER, idMenber);
            db.insert(FeedEntry.TABLE_NAME, null, values);
        }
    }

    public void deleteGroup(String idGroup){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(FeedEntry.TABLE_NAME, FeedEntry.COLUMN_GROUP_ID + " = " + idGroup , null);
    }

    public void addListGroup(ArrayList<Groups> listGroups) {
        for (Groups groups : listGroups) {
            addGroup(groups);
        }
    }

    public Groups getGroups(String id){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + GroupDataBase.FeedEntry.TABLE_NAME + " where " + FeedEntry.COLUMN_GROUP_ID +" = " + id, null);
        Groups newGroups = new Groups();
        while (cursor.moveToNext()) {
            String idGroup = cursor.getString(0);
            String nameGroup = cursor.getString(1);
            String admin = cursor.getString(2);
            String member = cursor.getString(3);
            newGroups.id = idGroup;
            newGroups.groupInfo.put("name", nameGroup);
            newGroups.groupInfo.put("admin", admin);
            newGroups.member.add(member);
        }
        return newGroups;
    }

    public ArrayList<Groups> getGroupsLisr() {
        Map<String, Groups> mapGroup = new HashMap<>();
        ArrayList<String> listKey = new ArrayList<>();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from " + GroupDataBase.FeedEntry.TABLE_NAME, null);
            while (cursor.moveToNext()) {
                String idGroup = cursor.getString(0);
                String nameGroup = cursor.getString(1);
                String admin = cursor.getString(2);
                String member = cursor.getString(3);
                if (!listKey.contains(idGroup)) {
                    Groups newGroups = new Groups();
                    newGroups.id = idGroup;
                    newGroups.groupInfo.put("name", nameGroup);
                    newGroups.groupInfo.put("admin", admin);
                    newGroups.member.add(member);
                    listKey.add(idGroup);
                    mapGroup.put(idGroup, newGroups);
                } else {
                    mapGroup.get(idGroup).member.add(member);
                }
            }
            cursor.close();
        } catch (Exception e) {
            return new ArrayList<Groups>();
        }

        ArrayList<Groups> listGroups = new ArrayList<>();
        for (String key : listKey) {
            listGroups.add(mapGroup.get(key));
        }

        return listGroups;
    }

    public void dropDB() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.execSQL(SQL_DELETE_ENTRIES);
        db.execSQL(SQL_CREATE_ENTRIES);
    }


    public static class FeedEntry implements BaseColumns {
        static final String TABLE_NAME = "groups";
        static final String COLUMN_GROUP_ID = "groupID";
        static final String COLUMN_GROUP_NAME = "name";
        static final String COLUMN_GROUP_ADMIN = "admin";
        static final String COLUMN_GROUP_MEMBER = "memberID";
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                    FeedEntry.COLUMN_GROUP_ID + TEXT_TYPE + COMMA_SEP +
                    FeedEntry.COLUMN_GROUP_NAME + TEXT_TYPE + COMMA_SEP +
                    FeedEntry.COLUMN_GROUP_ADMIN + TEXT_TYPE + COMMA_SEP +
                    GroupDataBase.FeedEntry.COLUMN_GROUP_MEMBER + TEXT_TYPE + COMMA_SEP +
                    "PRIMARY KEY (" + GroupDataBase.FeedEntry.COLUMN_GROUP_ID + COMMA_SEP +
                    GroupDataBase.FeedEntry.COLUMN_GROUP_MEMBER + "))";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + GroupDataBase.FeedEntry.TABLE_NAME;

    private static class GroupDBHelper extends SQLiteOpenHelper {
        static final int DATABASE_VERSION = 1;
        static final String DATABASE_NAME = "GroupChat.db";

        GroupDBHelper(Context context) {
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
