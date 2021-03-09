package server.storages;

import messages.Message;

import java.sql.SQLException;
import java.util.List;

public interface MessageStorage {
    void addMessage(int sender, int recipient, String message) throws SQLException;

    List<String[]> getMessage(int sender) throws SQLException;
}
