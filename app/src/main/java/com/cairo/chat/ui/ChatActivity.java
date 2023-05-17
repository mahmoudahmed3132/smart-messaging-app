package com.cairo.chat.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.cairo.chat.R;
import com.cairo.chat.data.SharedPreferenceHelper;
import com.cairo.chat.data.StaticConfig;
import com.cairo.chat.model.Conversation;
import com.cairo.chat.model.Messages;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    private RecyclerView recyclerChat;
    public static final int VIEW_TYPE_USER_MESSAGE = 0;
    public static final int VIEW_TYPE_FRIEND_MESSAGE = 1;
    private ListMessageAdapter adapter;
    private String roomId;
    private ArrayList<CharSequence> idFriend;
    private Conversation conversation;
    private ImageButton btnSend;
    private ImageView chooseFile;
    private EditText editWriteMessage;
    private LinearLayoutManager linearLayoutManager;
    public static HashMap<String, Bitmap> bitmapAvataFriend;
    public Bitmap bitmapAvataUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intentData = getIntent();
        idFriend = intentData.getCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID);
        roomId = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID);
        String nameFriend = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND);

        conversation = new Conversation();
        btnSend = (ImageButton) findViewById(R.id.btnSend);
        chooseFile = (ImageView) findViewById(R.id.chooseFile);
        btnSend.setOnClickListener(this);

        String base64AvataUser = SharedPreferenceHelper.getInstance(this).getUserInfo().avata;
        if (!base64AvataUser.equals(StaticConfig.STR_DEFAULT_BASE64)) {
            byte[] decodedString = Base64.decode(base64AvataUser, Base64.DEFAULT);
            bitmapAvataUser = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } else {
            bitmapAvataUser = null;
        }

        editWriteMessage = (EditText) findViewById(R.id.editWriteMessage);
        if (idFriend != null && nameFriend != null) {
            getSupportActionBar().setTitle(nameFriend);
            linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            recyclerChat = (RecyclerView) findViewById(R.id.recyclerChat);
            recyclerChat.setLayoutManager(linearLayoutManager);
            adapter = new ListMessageAdapter(this, conversation, bitmapAvataFriend, bitmapAvataUser);
            FirebaseDatabase.getInstance().getReference().child("message/" + roomId).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    if (dataSnapshot.getValue() != null) {
                        HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                        Messages newMessages = new Messages();
                        newMessages.idSender = (String) mapMessage.get("idSender");
                        newMessages.idReceiver = (String) mapMessage.get("idReceiver");
                        newMessages.text = (String) mapMessage.get("text");
                        newMessages.timestamp = (long) mapMessage.get("timestamp");
                        newMessages.type = (String) mapMessage.get("type");
                        newMessages.fileName = (String) mapMessage.get("fileName");
                        conversation.getListMessageData().add(newMessages);
                        adapter.notifyDataSetChanged();
                        linearLayoutManager.scrollToPosition(conversation.getListMessageData().size() - 1);
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
            recyclerChat.setAdapter(adapter);
        }

        chooseFile.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) pickFileFromStorage();
            else
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) performExternalStoragePermission();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            Intent result = new Intent();
            result.putExtra("idFriend", idFriend.get(0));
            setResult(RESULT_OK, result);
            this.finish();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("idFriend", idFriend.get(0));
        setResult(RESULT_OK, result);
        this.finish();
    }

    private void pickFileFromStorage() {
        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(
                Intent.createChooser(intent, "Select File"),
                205
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void performExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                   101
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickFileFromStorage();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    long fileSize = getFileSize(uri);
                    if (fileSize <= (100 * 1024 * 1024)) sendFile(uri);
                    else
                        Toast.makeText(this, "Selected file is too large. Please choose a file less than 100MB.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private long getFileSize(Uri fileUri) {
        String filePath = getPathFromUri(fileUri);
        File file = new File(filePath);
        if (file.exists()) {
            return file.length();
        }
        return 0;
    }

    /*private String getPathFromUri(Uri uri) {
        String filePath = null;
        String[] projection = {OpenableColumns.DISPLAY_NAME};
        try {
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                filePath = cursor.getString(columnIndex);
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }*/

    private String getPathFromUri(Uri uri) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        }

        return uri.getPath();
    }

    private void sendFile(Uri pdfFile) {
        if (pdfFile != null) {
            String timeStamp = String.valueOf(System.currentTimeMillis());
            String fileNameAndPath = "ChatFiles/" + "post_" + timeStamp;

            StorageReference firebaseStorageReference = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
            firebaseStorageReference.putFile(pdfFile)
                    .addOnSuccessListener(taskSnapshot -> {
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) {

                        }
                        if (uriTask.isSuccessful()) {
                            String downloadUri = uriTask.getResult().toString();

                            editWriteMessage.setText("");
                            Messages newMessages = new Messages();
                            newMessages.text = downloadUri;
                            newMessages.idSender = StaticConfig.UID;
                            newMessages.idReceiver = roomId;
                            newMessages.timestamp = System.currentTimeMillis();
                            newMessages.type = "file";
                            newMessages.fileName = "post_" + timeStamp;
                            FirebaseDatabase.getInstance().getReference().child("message/" + roomId).push().setValue(newMessages);
                        }
                    })
                    .addOnFailureListener(e -> {

                    });
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSend) {
            String content = editWriteMessage.getText().toString().trim();
            if (content.length() > 0) {
                editWriteMessage.setText("");
                Messages newMessages = new Messages();
                newMessages.text = content;
                newMessages.idSender = StaticConfig.UID;
                newMessages.idReceiver = roomId;
                newMessages.timestamp = System.currentTimeMillis();
                newMessages.type = "text";
                newMessages.fileName = "";
                FirebaseDatabase.getInstance().getReference().child("message/" + roomId).push().setValue(newMessages);
            }
        }
    }
}

class ListMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private Conversation conversation;
    private HashMap<String, Bitmap> bitmapAvata;
    private HashMap<String, DatabaseReference> bitmapAvataDB;
    private Bitmap bitmapAvataUser;

    public ListMessageAdapter(Context context, Conversation conversation, HashMap<String, Bitmap> bitmapAvata, Bitmap bitmapAvataUser) {
        this.context = context;
        this.conversation = conversation;
        this.bitmapAvata = bitmapAvata;
        this.bitmapAvataUser = bitmapAvataUser;
        bitmapAvataDB = new HashMap<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ChatActivity.VIEW_TYPE_FRIEND_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_friend, parent, false);
            return new ItemMessageFriendHolder(view);
        } else if (viewType == ChatActivity.VIEW_TYPE_USER_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_user, parent, false);
            return new ItemMessageUserHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemMessageFriendHolder) {

            if (conversation.getListMessageData().get(position).text.contains("https://firebasestorage.googleapis.com/v0/b/mychat-c632f.appspot.com/o/ChatFiles")) {
                ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.GONE);
                ((ItemMessageFriendHolder) holder).attachedFileIv.setVisibility(View.VISIBLE);
            } else {
                ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.VISIBLE);
                ((ItemMessageFriendHolder) holder).attachedFileIv.setVisibility(View.GONE);
                ((ItemMessageFriendHolder) holder).txtContent.setText(conversation.getListMessageData().get(position).text);
            }

            Bitmap currentAvata = bitmapAvata.get(conversation.getListMessageData().get(position).idSender);
            if (currentAvata != null) {
                ((ItemMessageFriendHolder) holder).avata.setImageBitmap(currentAvata);
            } else {
                final String id = conversation.getListMessageData().get(position).idSender;
                if(bitmapAvataDB.get(id) == null){
                    bitmapAvataDB.put(id, FirebaseDatabase.getInstance().getReference().child("user/" + id + "/avata"));
                    bitmapAvataDB.get(id).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                String avataStr = (String) dataSnapshot.getValue();
                                if(!avataStr.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                                    byte[] decodedString = Base64.decode(avataStr, Base64.DEFAULT);
                                    ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                                }else{
                                    ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avata));
                                }
                                notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }
            ((ItemMessageFriendHolder) holder).attachedFileIv.setOnClickListener(v -> {
                downloadPdfFromFirebaseStorage(conversation.getListMessageData().get(position).fileName);
            });
        } else if (holder instanceof ItemMessageUserHolder) {

            if (conversation.getListMessageData().get(position).text.contains("https://firebasestorage.googleapis.com/v0/b/mychat-c632f.appspot.com/o/ChatFiles")) {
                ((ItemMessageUserHolder) holder).txtContent.setVisibility(View.GONE);
                ((ItemMessageUserHolder) holder).attachedFileIv.setVisibility(View.VISIBLE);
            } else {
                ((ItemMessageUserHolder) holder).txtContent.setVisibility(View.VISIBLE);
                ((ItemMessageUserHolder) holder).attachedFileIv.setVisibility(View.GONE);
                ((ItemMessageUserHolder) holder).txtContent.setText(conversation.getListMessageData().get(position).text);
            }

            if (bitmapAvataUser != null) {
                ((ItemMessageUserHolder) holder).avata.setImageBitmap(bitmapAvataUser);
            }
            ((ItemMessageUserHolder) holder).attachedFileIv.setOnClickListener(v -> {
                downloadPdfFromFirebaseStorage(conversation.getListMessageData().get(position).fileName);
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return conversation.getListMessageData().get(position).idSender.equals(StaticConfig.UID) ? ChatActivity.VIEW_TYPE_USER_MESSAGE : ChatActivity.VIEW_TYPE_FRIEND_MESSAGE;
    }

    @Override
    public int getItemCount() {
        return conversation.getListMessageData().size();
    }

    private void downloadPdfFromFirebaseStorage(String pdfFileName) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        StorageReference pdfRef = storageRef.child("ChatFiles/" + pdfFileName);
        File localFile;
        try {
            localFile = File.createTempFile("temp_pdf", ".pdf");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        pdfRef.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    openPdfFile(localFile);
                })
                .addOnFailureListener(exception -> {
                    exception.printStackTrace();
                });
    }

    private void openPdfFile(File pdfFile) {
        Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", pdfFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(contentUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PackageManager packageManager = context.getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "No PDF viewer app found.", Toast.LENGTH_SHORT).show();
        }
    }
}

class ItemMessageUserHolder extends RecyclerView.ViewHolder {
    public TextView txtContent;
    public CircleImageView avata;
    public ImageView attachedFileIv;

    public ItemMessageUserHolder(View itemView) {
        super(itemView);
        txtContent = (TextView) itemView.findViewById(R.id.textContentUser);
        avata = (CircleImageView) itemView.findViewById(R.id.imageView2);
        attachedFileIv = (ImageView) itemView.findViewById(R.id.attachedFileIv);
    }
}

class ItemMessageFriendHolder extends RecyclerView.ViewHolder {
    public TextView txtContent;
    public CircleImageView avata;
    public ImageView attachedFileIv;

    public ItemMessageFriendHolder(View itemView) {
        super(itemView);
        txtContent = (TextView) itemView.findViewById(R.id.textContentFriend);
        avata = (CircleImageView) itemView.findViewById(R.id.imageView3);
        attachedFileIv = (ImageView) itemView.findViewById(R.id.attachedFileIv);
    }
}
