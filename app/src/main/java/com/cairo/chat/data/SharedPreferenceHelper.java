package com.cairo.chat.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.cairo.chat.model.Users;

public class SharedPreferenceHelper {
    private static SharedPreferenceHelper instance = null;
    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;
    private static String SHARE_USER_INFO = "userinfo";
    private static String SHARE_KEY_NAME = "name";
    private static String SHARE_KEY_EMAIL = "email";
    private static String SHARE_KEY_AVATA = "avatar";
    private static String SHARE_KEY_UID = "uid";


    private SharedPreferenceHelper() {}

    public static SharedPreferenceHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferenceHelper();
            preferences = context.getSharedPreferences(SHARE_USER_INFO, Context.MODE_PRIVATE);
            editor = preferences.edit();
        }
        return instance;
    }

    public void saveUserInfo(Users users) {
        editor.putString(SHARE_KEY_NAME, users.name);
        editor.putString(SHARE_KEY_EMAIL, users.email);
        editor.putString(SHARE_KEY_AVATA, users.avata);
        editor.putString(SHARE_KEY_UID, StaticConfig.UID);
        editor.apply();
    }

    public Users getUserInfo(){
        String userName = preferences.getString(SHARE_KEY_NAME, "");
        String email = preferences.getString(SHARE_KEY_EMAIL, "");
        String avatar = preferences.getString(SHARE_KEY_AVATA, "default");

        Users users = new Users();
        users.name = userName;
        users.email = email;
        users.avata = avatar;

        return users;
    }

    public String getUID(){
        return preferences.getString(SHARE_KEY_UID, "");
    }

}
