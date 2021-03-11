package server.storages;

public interface UserStorage {
    void add(String login, String password, String nick);

    void remove(String login);

    boolean loginExist(String login);

    boolean nickExist(String nick);

    String login(String login, String password);

    default void changeNick(String nick, String newNick) {
        throw new UnsupportedOperationException();
    };

    default void disconnected() {
        throw new UnsupportedOperationException();
    };
}
