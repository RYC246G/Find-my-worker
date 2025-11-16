package com.example.findmyworker;

public class Chat {
    private Chat next;
    private String data;


    public Chat(Chat next, String data) {
        this.next = next;
        this.data = data;
    }

    public Chat getNext() {
        return next;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setNext(Chat next) {
        this.next = next;
    }
}


