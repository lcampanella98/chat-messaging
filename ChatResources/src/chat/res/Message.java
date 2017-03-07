package chat.res;

import chat.res.sender.Sender;

import java.io.Serializable;

public class Message implements Serializable {

    private Sender sender, receiver;
    private String text;
    private MessageType msgType;

    public Message(Sender sender, String text, MessageType messageType) {
        this(sender, null, text, messageType);
    }

    public Message(Sender sender, MessageType messageType) {
        this(sender, null, "", messageType);
    }

    public Message(Sender sender, Sender receiver, MessageType messageType) {
        this(sender, receiver, "", messageType);
    }

    public Message(Sender sender, Sender receiver, String text, MessageType messageType) {
        setMessageType(messageType);
        setSender(sender);
        setReceiver(receiver);
        setMessageText(text);
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
        return msgType + ((text != null && !text.isEmpty()) ? " : " + text : "");
    }

}
