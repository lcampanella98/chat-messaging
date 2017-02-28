package chat.res;

import chat.res.sender.Sender;

import java.io.Serializable;

public class Message implements Serializable {

    private Sender sender, receiver;
    private String text;
    private MessageType msgType;

    public Message(Sender sender, String text, MessageType messageType) {
        this.sender = sender;
        this.text = text;
        msgType = messageType;
    }

    public Message(Sender sender, MessageType messageType) {
        this.sender = sender;
        msgType = messageType;
    }

    public Message(Sender sender, String text, MessageType messageType, Sender receiver) {
        msgType = messageType;
        this.sender = sender;
        setMessageText(text);
        setReceiver(receiver);
    }

    public Message(Sender sender, MessageType messageType, Sender receiver) {
        this.sender = sender;
        msgType = messageType;
        this.receiver = receiver;
    }

    public Sender getSender() {
        return sender;
    }

    public String getMessageText() {
        return text;
    }

    public MessageType getMessageType() {
        return msgType;
    }

    public Sender getReceiver() {
        return receiver;
    }

    public void setReceiver(Sender receiver) {
        this.receiver = receiver;
    }

    public void setMessageType(MessageType msgType) {
        this.msgType = msgType;
    }

    public void setSender(Sender user) {
        this.sender = user;
    }

    public void setMessageText(String text) {
        this.text = text;
    }

    public String toString() {
        return msgType.toString() + (text != null ? " : " + text : "");
    }

}
