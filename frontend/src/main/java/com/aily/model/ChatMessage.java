package com.aily.model;

public class ChatMessage {
    public enum Sender { USER, BOT }

    private final Sender sender;
    private final String text;
    private final String time;

    public ChatMessage(Sender sender, String text, String time) {
        this.sender = sender;
        this.text   = text;
        this.time   = time;
    }

    public Sender getSender() { return sender; }
    public String getText()   { return text; }
    public String getTime()   { return time; }
}
