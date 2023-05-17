package com.cairo.chat.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.util.Base64;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.cairo.chat.MainActivity;
import com.cairo.chat.R;
import com.cairo.chat.data.FriendDataBase;
import com.cairo.chat.data.GroupDataBase;
import com.cairo.chat.data.StaticConfig;
import com.cairo.chat.model.Friends;
import com.cairo.chat.model.Groups;
import com.cairo.chat.model.FriendsList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;



public class FriendChatService extends Service {
    private static String TAG = "FriendChatService";
    public final IBinder mBinder = new LocalBinder();
    public Map<String, Boolean> mapMark;
    public Map<String, Query> mapQuery;
    public Map<String, ChildEventListener> mapChildEventListenerMap;
    public Map<String, Bitmap> mapBitmap;
    public ArrayList<String> listKey;
    public FriendsList friendsList;
    public ArrayList<Groups> listGroups;
    public CountDownTimer updateOnline;

    public FriendChatService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mapMark = new HashMap<>();
        mapQuery = new HashMap<>();
        mapChildEventListenerMap = new HashMap<>();
        friendsList = FriendDataBase.getInstance(this).getFriendsList();
        listGroups = GroupDataBase.getInstance(this).getGroupsLisr();
        listKey = new ArrayList<>();
        mapBitmap = new HashMap<>();
        updateOnline = new CountDownTimer(System.currentTimeMillis(), StaticConfig.TIME_TO_REFRESH) {
            @Override
            public void onTick(long l) {
                ServiceUtils.updateUserStatus(getApplicationContext());
            }

            @Override
            public void onFinish() {

            }
        };
        updateOnline.start();

        if (friendsList.getFriendsList().size() > 0 || listGroups.size() > 0) {
            for (final Friends friends : friendsList.getFriendsList()) {
                if (!listKey.contains(friends.idRoom)) {
                    mapQuery.put(friends.idRoom, FirebaseDatabase.getInstance().getReference().child("message/" + friends.idRoom).limitToLast(1));
                    mapChildEventListenerMap.put(friends.idRoom, new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            if (mapMark.get(friends.idRoom) != null && mapMark.get(friends.idRoom)) {
                                if (mapBitmap.get(friends.idRoom) == null) {
                                    if (!friends.avata.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                                        byte[] decodedString = Base64.decode(friends.avata, Base64.DEFAULT);
                                        mapBitmap.put(friends.idRoom, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                                    } else {
                                        mapBitmap.put(friends.idRoom, BitmapFactory.decodeResource(getResources(), R.drawable.default_avata));
                                    }
                                }
                                createNotify(friends.name, (String) ((HashMap) dataSnapshot.getValue()).get("text"), friends.idRoom.hashCode(), mapBitmap.get(friends.idRoom), false);

                            } else {
                                mapMark.put(friends.idRoom, true);
                            }
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    listKey.add(friends.idRoom);
                }
                mapQuery.get(friends.idRoom).addChildEventListener(mapChildEventListenerMap.get(friends.idRoom));
            }

            for (final Groups groups : listGroups) {
                if (!listKey.contains(groups.id)) {
                    mapQuery.put(groups.id, FirebaseDatabase.getInstance().getReference().child("message/" + groups.id).limitToLast(1));
                    mapChildEventListenerMap.put(groups.id, new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            if (mapMark.get(groups.id) != null && mapMark.get(groups.id)) {
                                if (mapBitmap.get(groups.id) == null) {
                                    mapBitmap.put(groups.id, BitmapFactory.decodeResource(getResources(), R.drawable.ic_notify_group));
                                }
                                createNotify(groups.groupInfo.get("name"), (String) ((HashMap) dataSnapshot.getValue()).get("text"), groups.id.hashCode(), mapBitmap.get(groups.id) , true);
                            } else {
                                mapMark.put(groups.id, true);
                            }
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    listKey.add(groups.id);
                }
                mapQuery.get(groups.id).addChildEventListener(mapChildEventListenerMap.get(groups.id));
            }

        } else {
            stopSelf();
        }
    }

    public void stopNotify(String id) {
        mapMark.put(id, false);
    }

    public void createNotify(String name, String content, int id, Bitmap icon, boolean isGroup) {
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder notificationBuilder = new
                NotificationCompat.Builder(this)
                .setLargeIcon(icon)
                .setContentTitle(name)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[] { 1000, 1000})
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setAutoCancel(true);
        if (isGroup) {
            notificationBuilder.setSmallIcon(R.drawable.ic_tab_group);
        } else {
            notificationBuilder.setSmallIcon(R.drawable.ic_tab_person);
        }
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
        notificationManager.notify(id,
                notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OnStartService");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "OnBindService");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (String id : listKey) {
            mapQuery.get(id).removeEventListener(mapChildEventListenerMap.get(id));
        }
        mapQuery.clear();
        mapChildEventListenerMap.clear();
        mapBitmap.clear();
        updateOnline.cancel();
        Log.d(TAG, "OnDestroyService");
    }

    public class LocalBinder extends Binder {
        public FriendChatService getService() {
            return FriendChatService.this;
        }
    }
}
