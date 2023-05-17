package com.cairo.chat.model;



public class Groups extends Room{
    public String id;
    public FriendsList friendsList;

    public Groups(){
        friendsList = new FriendsList();
    }
}
