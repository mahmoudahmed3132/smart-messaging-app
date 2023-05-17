package com.cairo.chat.model;

import java.util.ArrayList;


public class FriendsList {
    private ArrayList<Friends> listFriends;

    public ArrayList<Friends> getFriendsList() {
        return listFriends;
    }

    public FriendsList(){
        listFriends = new ArrayList<>();
    }

    public String getAvataById(String id){
        for(Friends friends : listFriends){
            if(id.equals(friends.id)){
                return friends.avata;
            }
        }
        return "";
    }

    public void setListFriend(ArrayList<Friends> listFriends) {
        this.listFriends = listFriends;
    }
}
