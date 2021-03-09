package server.storages;

import comands.ReasonAuthExceptions;
import server.exceptions.AuthExceptions;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;

public class SimpleUserStorage implements UserStorage {
    private class User {
        private final String login;
        private final String password;
        private final String nick;

        public User(String login, String password, String nick) {
            this.login = login;
            this.password = password;
            this.nick = nick;
        }
    }

    private CopyOnWriteArraySet<User> users;

    public SimpleUserStorage() {
        users = new CopyOnWriteArraySet<>();

        users.add(new User("qwe", "qwe", "qwe"));
        users.add(new User("asd", "asd", "asd"));
        users.add(new User("zxc", "zxc", "zxc"));
    }

    @Override
    public void add(String login, String password, String nick) {
        if (loginExist(login) && nickExist(nick)) {
            throw new AuthExceptions(ReasonAuthExceptions.LOGIN_AND_NICK_EXIST);
        } else if (loginExist(login)) {
            throw new AuthExceptions(ReasonAuthExceptions.LOGIN_EXIST);
        } else if (nickExist(nick)) {
            throw new AuthExceptions(ReasonAuthExceptions.NICK_EXIST);
        }

        users.add(new User(login.toLowerCase(), password, nick));
    }

    @Override
    public void remove(String login) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean loginExist(String login) {
        return users.stream()
                .map(u -> u.login)
                .anyMatch(l -> l.equals(login.toLowerCase()));
    }

    @Override
    public boolean nickExist(String nick) {
        return users.stream()
                .map(u -> u.login)
                .map(String::toLowerCase)
                .anyMatch(n -> n.equals(nick.toLowerCase()));
    }

    @Override
    public Client login(String login, String password) {
        Optional<String> nick =  users.stream()
                .filter(u -> u.login.equals(login.toLowerCase()) && u.password.equals(password))
                .map(u -> u.nick)
                .findFirst();

        if (!nick.isPresent()) {
            throw new AuthExceptions(ReasonAuthExceptions.INCORRECT_LOGIN_OR_PASS);
        }

//        return nick.get();
        return new Client(0, "");
    }
}
