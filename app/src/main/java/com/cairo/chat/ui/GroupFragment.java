package com.cairo.chat.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cairo.chat.model.Groups;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.cairo.chat.R;
import com.cairo.chat.data.FriendDataBase;
import com.cairo.chat.data.GroupDataBase;
import com.cairo.chat.data.StaticConfig;
import com.cairo.chat.model.FriendsList;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

@SuppressWarnings("unchecked")
public class GroupFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{
    private RecyclerView recyclerListGroups;
    public FragGroupClickFloatButton onClickFloatButton;
    private ArrayList<Groups> listGroups;
    private ListGroupsAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    public static final int CONTEXT_MENU_DELETE = 1;
    public static final int CONTEXT_MENU_EDIT = 2;
    public static final int CONTEXT_MENU_LEAVE = 3;
    public static final int REQUEST_EDIT_GROUP = 0;
    public static final String CONTEXT_MENU_KEY_INTENT_DATA_POS = "pos";

    LovelyProgressDialog progressDialog, waitingLeavingGroup;

    public GroupFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_group, container, false);

        listGroups = GroupDataBase.getInstance(getContext()).getGroupsLisr();
        recyclerListGroups = (RecyclerView) layout.findViewById(R.id.recycleListGroup);
        mSwipeRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerListGroups.setLayoutManager(layoutManager);
        adapter = new ListGroupsAdapter(getContext(), listGroups);
        recyclerListGroups.setAdapter(adapter);
        onClickFloatButton = new FragGroupClickFloatButton();
        progressDialog = new LovelyProgressDialog(getContext())
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_delete_group)
                .setTitle("Deleting....")
                .setTopColorRes(R.color.colorAccent);

        waitingLeavingGroup = new LovelyProgressDialog(getContext())
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_delete_group)
                .setTitle("Group leaving....")
                .setTopColorRes(R.color.colorAccent);

        if(listGroups.size() == 0){
            mSwipeRefreshLayout.setRefreshing(true);
            getListGroup();
        }
        return layout;
    }

    private void getListGroup(){
        FirebaseDatabase.getInstance().getReference().child("user/"+ StaticConfig.UID+"/group").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null) {
                    HashMap mapListGroup = (HashMap) dataSnapshot.getValue();
                    Iterator iterator = mapListGroup.keySet().iterator();
                    while (iterator.hasNext()){
                        String idGroup = (String) mapListGroup.get(iterator.next().toString());
                        Groups newGroups = new Groups();
                        newGroups.id = idGroup;
                        listGroups.add(newGroups);
                    }
                    getGroupsInfo(0);
                }else{
                    mSwipeRefreshLayout.setRefreshing(false);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_EDIT_GROUP && resultCode == Activity.RESULT_OK) {
            listGroups.clear();
            ListGroupsAdapter.friendsList = null;
            GroupDataBase.getInstance(getContext()).dropDB();
            getListGroup();
        }
    }

    private void getGroupsInfo(final int indexGroup){
        if(indexGroup == listGroups.size()){
            adapter.notifyDataSetChanged();
            mSwipeRefreshLayout.setRefreshing(false);
        }else {
            FirebaseDatabase.getInstance().getReference().child("group/"+ listGroups.get(indexGroup).id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getValue() != null){
                        HashMap mapGroup = (HashMap) dataSnapshot.getValue();
                        ArrayList<String> member = (ArrayList<String>) mapGroup.get("member");
                        HashMap mapGroupInfo = (HashMap) mapGroup.get("groupInfo");
                        for(String idMember: member){
                            listGroups.get(indexGroup).member.add(idMember);
                        }
                        listGroups.get(indexGroup).groupInfo.put("name", (String) mapGroupInfo.get("name"));
                        listGroups.get(indexGroup).groupInfo.put("admin", (String) mapGroupInfo.get("admin"));
                    }
                    GroupDataBase.getInstance(getContext()).addGroup(listGroups.get(indexGroup));
                    Log.d("GroupFragment", listGroups.get(indexGroup).id +": " + dataSnapshot.toString());
                    getGroupsInfo(indexGroup +1);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    @Override
    public void onRefresh() {
        listGroups.clear();
        ListGroupsAdapter.friendsList = null;
        GroupDataBase.getInstance(getContext()).dropDB();
        adapter.notifyDataSetChanged();
        getListGroup();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case CONTEXT_MENU_DELETE:
                int posGroup = item.getIntent().getIntExtra(CONTEXT_MENU_KEY_INTENT_DATA_POS, -1);
                if(((String) listGroups.get(posGroup).groupInfo.get("admin")).equals(StaticConfig.UID)) {
                    Groups groups = listGroups.get(posGroup);
                    listGroups.remove(posGroup);
                    if(groups != null){
                        deleteGroup(groups, 0);
                    }
                }else{
                    Toast.makeText(getActivity(), "You are not admin", Toast.LENGTH_LONG).show();
                }
                break;
            case CONTEXT_MENU_EDIT:
                int posGroup1 = item.getIntent().getIntExtra(CONTEXT_MENU_KEY_INTENT_DATA_POS, -1);
                if(((String) listGroups.get(posGroup1).groupInfo.get("admin")).equals(StaticConfig.UID)) {
                    Intent intent = new Intent(getContext(), AddGroupActivity.class);
                    intent.putExtra("groupId", listGroups.get(posGroup1).id);
                    startActivityForResult(intent, REQUEST_EDIT_GROUP);
                }else{
                    Toast.makeText(getActivity(), "You are not admin", Toast.LENGTH_LONG).show();
                }

                break;

            case CONTEXT_MENU_LEAVE:
                int position = item.getIntent().getIntExtra(CONTEXT_MENU_KEY_INTENT_DATA_POS, -1);
                if(((String) listGroups.get(position).groupInfo.get("admin")).equals(StaticConfig.UID)) {
                    Toast.makeText(getActivity(), "Admin cannot leave group", Toast.LENGTH_LONG).show();
                }else{
                    waitingLeavingGroup.show();
                    Groups groupsLeaving = listGroups.get(position);
                    leaveGroup(groupsLeaving);
                }
                break;
        }

        return super.onContextItemSelected(item);
    }

    public void deleteGroup(final Groups groups, final int index){
        if(index == groups.member.size()){
            FirebaseDatabase.getInstance().getReference().child("group/"+ groups.id).removeValue()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            progressDialog.dismiss();
                            GroupDataBase.getInstance(getContext()).deleteGroup(groups.id);
                            listGroups.remove(groups);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(getContext(), "Deleted group", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        new LovelyInfoDialog(getContext())
                                .setTopColorRes(R.color.colorAccent)
                                .setIcon(R.drawable.ic_dialog_delete_group)
                                .setTitle("False")
                                .setMessage("Cannot delete group right now, please try again.")
                                .setCancelable(false)
                                .setConfirmButtonText("Ok")
                                .show();
                    })
                    ;
        }else{
            FirebaseDatabase.getInstance().getReference().child("user/"+ groups.member.get(index)+"/group/"+ groups.id).removeValue()
                    .addOnCompleteListener(task -> deleteGroup(groups, index + 1))
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            new LovelyInfoDialog(getContext())
                                    .setTopColorRes(R.color.colorAccent)
                                    .setIcon(R.drawable.ic_dialog_delete_group)
                                    .setTitle("False")
                                    .setMessage("Cannot connect server")
                                    .setCancelable(false)
                                    .setConfirmButtonText("Ok")
                                    .show();
                        }
                    });
        }
    }

    public void leaveGroup(final Groups groups){
        FirebaseDatabase.getInstance().getReference().child("group/"+ groups.id+"/member")
                .orderByValue().equalTo(StaticConfig.UID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getValue() == null) {
                            waitingLeavingGroup.dismiss();
                            new LovelyInfoDialog(getContext())
                                    .setTopColorRes(R.color.colorAccent)
                                    .setTitle("Error")
                                    .setMessage("Error occurred during leaving group")
                                    .show();
                        } else {
                            String memberIndex = "";
                            ArrayList<String> result = ((ArrayList<String>)dataSnapshot.getValue());
                            for(int i = 0; i < result.size(); i++){
                                if(result.get(i) != null){
                                    memberIndex = String.valueOf(i);
                                }
                            }

                            FirebaseDatabase.getInstance().getReference().child("user").child(StaticConfig.UID)
                                    .child("group").child(groups.id).removeValue();
                            FirebaseDatabase.getInstance().getReference().child("group/"+ groups.id+"/member")
                                    .child(memberIndex).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            waitingLeavingGroup.dismiss();

                                            listGroups.remove(groups);
                                            adapter.notifyDataSetChanged();
                                            GroupDataBase.getInstance(getContext()).deleteGroup(groups.id);
                                            new LovelyInfoDialog(getContext())
                                                    .setTopColorRes(R.color.colorAccent)
                                                    .setTitle("Success")
                                                    .setMessage("Group leaving successfully")
                                                    .show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        waitingLeavingGroup.dismiss();
                                        new LovelyInfoDialog(getContext())
                                                .setTopColorRes(R.color.colorAccent)
                                                .setTitle("Error")
                                                .setMessage("Error occurred during leaving group")
                                                .show();
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        waitingLeavingGroup.dismiss();
                        new LovelyInfoDialog(getContext())
                                .setTopColorRes(R.color.colorAccent)
                                .setTitle("Error")
                                .setMessage("Error occurred during leaving group")
                                .show();
                    }
                });

    }

    public class FragGroupClickFloatButton implements View.OnClickListener{

        Context context;
        public FragGroupClickFloatButton getInstance(Context context){
            this.context = context;
            return this;
        }

        @Override
        public void onClick(View view) {
            startActivity(new Intent(getContext(), AddGroupActivity.class));
        }
    }
}
class ListGroupsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Groups> listGroups;
    public static FriendsList friendsList = null;
    private Context context;

    public ListGroupsAdapter(Context context,ArrayList<Groups> listGroups){
        this.context = context;
        this.listGroups = listGroups;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_group, parent, false);
        return new ItemGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final String groupName = listGroups.get(position).groupInfo.get("name");
        if(groupName != null && groupName.length() > 0) {
            ((ItemGroupViewHolder) holder).txtGroupName.setText(groupName);
            ((ItemGroupViewHolder) holder).iconGroup.setText((groupName.charAt(0) + "").toUpperCase());
        }
        ((ItemGroupViewHolder) holder).btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setTag(new Object[]{groupName, position});
                view.getParent().showContextMenuForChild(view);
            }
        });
        ((RelativeLayout)((ItemGroupViewHolder) holder).txtGroupName.getParent()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(friendsList == null){
                    friendsList = FriendDataBase.getInstance(context).getFriendsList();
                }
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, groupName);
                ArrayList<CharSequence> idFriend = new ArrayList<>();
                ChatActivity.bitmapAvataFriend = new HashMap<>();
                for(String id : listGroups.get(position).member) {
                    idFriend.add(id);
                    String avata = friendsList.getAvataById(id);
                    if(!avata.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                        byte[] decodedString = Base64.decode(avata, Base64.DEFAULT);
                        ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                    }else if(avata.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                        ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avata));
                    }else {
                        ChatActivity.bitmapAvataFriend.put(id, null);
                    }
                }
                intent.putCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID, idFriend);
                intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, listGroups.get(position).id);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listGroups.size();
    }
}

class ItemGroupViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
    public TextView iconGroup, txtGroupName;
    public ImageButton btnMore;
    public ItemGroupViewHolder(View itemView) {
        super(itemView);
        itemView.setOnCreateContextMenuListener(this);
        iconGroup = (TextView) itemView.findViewById(R.id.icon_group);
        txtGroupName = (TextView) itemView.findViewById(R.id.txtName);
        btnMore = (ImageButton) itemView.findViewById(R.id.btnMoreAction);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        menu.setHeaderTitle((String) ((Object[])btnMore.getTag())[0]);
        Intent data = new Intent();
        data.putExtra(GroupFragment.CONTEXT_MENU_KEY_INTENT_DATA_POS, (Integer) ((Object[])btnMore.getTag())[1]);
        menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_EDIT, Menu.NONE, "Edit group").setIntent(data);
        menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_DELETE, Menu.NONE, "Delete group").setIntent(data);
        menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_LEAVE, Menu.NONE, "Leave group").setIntent(data);
    }
}