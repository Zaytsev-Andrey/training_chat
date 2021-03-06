package messages;

import comands.ReasonAuthExceptions;
import parameters.ParameterBD;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Message implements Serializable {
    private String type;
    private String text;
    private String login;
    private String password;
    private String nick;
    private String newNick;
    private String sender;
    private String recipient;
    private String reasonAuthException;
    private List<String> activeUser;
    private List<Message> messageList;
    private Date date;

    private Message() {

    }

    public static Message createAuthMessage(String login, String password) {
        Message message = new Message();
        message.type = MessageType.AUTH.name();
        message.login = login;
        message.password = password;
        return message;
    }

    public static Message createAuthOkMessage(String nick) {
        Message message = new Message();
        message.type = MessageType.AUTH_OK.name();
        message.nick = nick;
        return message;
    }

    public static Message createAuthFailMessage(ReasonAuthExceptions reason) {
        Message message = new Message();
        message.type = MessageType.AUTH_FAIL.name();
        message.reasonAuthException = reason.name();
        return message;
    }

    public static Message createRegMessage(String login, String password, String nick) {
        Message message = new Message();
        message.type = MessageType.REG.name();
        message.login = login;
        message.password = password;
        message.nick = nick;
        return message;
    }

    public static Message createRegOkMessage() {
        Message message = new Message();
        message.type = MessageType.REG_OK.name();
        return message;
    }

    public static Message createRegFailMessage(ReasonAuthExceptions reason) {
        Message message = new Message();
        message.type = MessageType.REG_FAIL.name();
        message.reasonAuthException = reason.name();
        return message;
    }

    public static Message createChangeNickMessage(String newNick) {
        Message message = new Message();
        message.type = MessageType.CHANGE_NICK.name();
        message.newNick = newNick;

        return message;
    }

    public static Message createChangeNickOkMessage(String newNick) {
        Message message = new Message();
        message.type = MessageType.CHANGE_NICK_OK.name();
        message.nick = newNick;

        return message;
    }

    public static Message createChangeNickFailMessage(ReasonAuthExceptions reason) {
        Message message = new Message();
        message.type = MessageType.CHANGE_NICK_FAIL.name();
        message.reasonAuthException = reason.name();

        return message;
    }

    public static Message createTextMessage(String sender, String text) {
        Message message = new Message();
        message.type = MessageType.TEXT.name();
        message.text = text;
        message.sender = sender;
        message.recipient = ParameterBD.ALL_USER_NICK;
        message.date = new Date();
        return message;
    }

    public static Message createPrivateTextMessage(String text, String sender, String recipient) {
        Message message = new Message();
        message.type = MessageType.PRIVATE_TEXT.name();
        message.text = text;
        message.sender = sender;
        message.recipient = recipient;
        return message;
    }

    public static Message createGetTextMessage(String sender) {
        Message message = new Message();
        message.type = MessageType.GET_TEXT.name();
        message.sender = sender;
        return message;
    }

    public static Message createTextListMessage(List<Message> messageList) {
        Message message = new Message();
        message.type = MessageType.TEXT_LIST.name();
        message.messageList = messageList;
        return message;
    }

    public static Message createUserListMessage(List<String> userList) {
        Message message = new Message();
        message.type = MessageType.USER_LIST.name();
        message.activeUser = new ArrayList<>(userList);
        return message;
    }

    public static Message createEndMessage() {
        Message message = new Message();
        message.type = MessageType.END.name();
        return message;
    }

    public MessageType getMessageType() {
        return MessageType.valueOf(type);
    }

    public String getText() {
        return text;
    }

    public String getSender() {
        return sender;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getNick() {
        return nick;
    }

    public String getNewNick() {
        return newNick;
    }

    public String getReasonMessage() {
        return ReasonAuthExceptions.valueOf(reasonAuthException).getMessage();
    }

    public ReasonAuthExceptions getReason() {
        return ReasonAuthExceptions.valueOf(reasonAuthException);
    }

    public List<String> getActiveUser() {
        return activeUser;
    }

    public String getRecipient() {
        return recipient;
    }

    public Date getDate() {
        return date;
    }

    public List<Message> getMessageList() {
        return messageList;
    }
}
