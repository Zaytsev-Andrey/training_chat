package server.storages;

import java.sql.SQLException;

public interface UserStorage {
    void add(String login, String password, String nick);

    void remove(String login);

    boolean loginExist(String login);

    boolean nickExist(String nick);

    Client login(String login, String password);

    default void changeNick(int id, String newNick) {
        throw new UnsupportedOperationException();
    };

    default int getIdForNick(String string) throws SQLException {
        throw new UnsupportedOperationException();
    }

    default void disconnected() {
        throw new UnsupportedOperationException();
    };
}
