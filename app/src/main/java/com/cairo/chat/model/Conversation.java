package com.cairo.chat.model;

import java.util.ArrayList;



public class Conversation {
    private ArrayList<Messages> listMessagesData;
    public Conversation(){
        listMessagesData = new ArrayList<>();
    }

    public ArrayList<Messages> getListMessageData() {
        return listMessagesData;
    }
}
