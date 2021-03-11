package server.storages;

import messages.Message;

import java.sql.SQLException;
import java.util.List;

public interface MessageStorage {
    void addMessage(String sender, String recipient, String message) throws SQLException;

    List<Message> getMessageList(String sender) throws SQLException;
}
