package com.cairo.chat.model;



public class Users {
    public String name;
    public String email;
    public String avata;
    public Status status;
    public Messages messages;

    public Users(){
        status = new Status();
        messages = new Messages();
        status.isOnline = false;
        status.timestamp = 0;
        messages.idReceiver = "0";
        messages.idSender = "0";
        messages.text = "";
        messages.timestamp = 0;
        messages.type = "";
        messages.fileName = "";
    }
}
