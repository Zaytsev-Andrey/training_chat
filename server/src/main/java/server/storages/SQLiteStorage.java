package server.storages;

import comands.ReasonAuthExceptions;
import messages.Message;
import parameters.ParameterBD;
import server.exceptions.AuthExceptions;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLiteStorage implements UserStorage, MessageStorage {
    private Connection connection;
    private Statement stmt;
    private PreparedStatement prStmtLogin;
    private PreparedStatement prStmtRegister;
    private PreparedStatement prStmtUpdateNick;
    private PreparedStatement prStmtAddMsg;
    private PreparedStatement prStmtGetMsg;

    public SQLiteStorage() throws ClassNotFoundException, SQLException {
        connection();

        // Авторизация
        prStmtLogin = connection.prepareStatement("SELECT nick FROM users WHERE login = ? AND password = ?;");

        // Регистрация
        prStmtRegister = connection.prepareStatement("INSERT INTO users (login, password, nick) VALUES (?, ?, ?);");

        // Изменение nick
        prStmtUpdateNick = connection.prepareStatement("UPDATE users SET nick = ? WHERE nick = ?;");

        // Добавление сообщения
        prStmtAddMsg = connection.prepareStatement("INSERT INTO messages (sender_id, recipient_id, message) VALUES ( \n" +
                "    (SELECT id FROM users WHERE nick = ?),\n" +
                "    (SELECT id FROM users WHERE nick = ?),\n" +
                "    ?);");

        // Запрос сообщения
        prStmtGetMsg = connection.prepareStatement("SELECT snd.nick AS sender, rcp.nick AS recipient, message \n" +
                "    FROM messages AS msg\n" +
                "    LEFT JOIN users AS snd ON (snd.id = msg.sender_id)\n" +
                "    LEFT JOIN users AS rcp ON (rcp.id = msg.recipient_id)\n" +
                "    WHERE snd.nick = ? OR rcp.nick = ? OR rcp.nick = ?;");

    }

    public void connection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:chat.db");
        stmt = connection.createStatement();
    }

    public void disconnected() {
        try {
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(String login, String password, String nick) {
        try {
            prStmtRegister.setString(1, login.toLowerCase());
            prStmtRegister.setString(2, password);
            prStmtRegister.setString(3, nick.toLowerCase());
            prStmtRegister.executeUpdate();
        } catch (SQLException e) {
            throw new AuthExceptions(ReasonAuthExceptions.LOGIN_OR_NICK_EXIST);
        }

    }

    @Override
    public void remove(String login) {

    }

    @Override
    public boolean loginExist(String login) {
        return false;
    }

    @Override
    public boolean nickExist(String nick) {
        return false;
    }

    @Override
    public String login(String login, String password) {
        ResultSet resultSet = null;
        String nick;
        try {
            prStmtLogin.setString(1, login.toLowerCase());
            prStmtLogin.setString(2, password);
            resultSet = prStmtLogin.executeQuery();

            if (resultSet.next()) {
                nick = resultSet.getString("nick");
            } else {
                throw new AuthExceptions(ReasonAuthExceptions.INCORRECT_LOGIN_OR_PASS);
            }
        } catch (SQLException e) {
            throw new AuthExceptions(ReasonAuthExceptions.AUTH_ERROR);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return nick;
    }

    @Override
    public void changeNick(String nick, String newNick) {
        try {
            prStmtUpdateNick.setString(1, newNick.toLowerCase());
            prStmtUpdateNick.setString(2, nick);
            prStmtUpdateNick.executeUpdate();
        } catch (SQLException e) {
            throw new AuthExceptions(ReasonAuthExceptions.NICK_EXIST);
        }
    }

    @Override
    public void addMessage(String sender, String recipient, String message) throws SQLException {
        prStmtAddMsg.setString(1, sender);
        prStmtAddMsg.setString(2, recipient);
        prStmtAddMsg.setString(3, message);
        prStmtAddMsg.executeUpdate();
    }

    @Override
    public List<Message> getMessageList(String sender) throws SQLException {
        prStmtGetMsg.setString(1, sender);
        prStmtGetMsg.setString(2, sender);
        prStmtGetMsg.setString(3, ParameterBD.ALL_USER_NICK);
        ResultSet resultSet = prStmtGetMsg.executeQuery();

        List<Message> messageList = new ArrayList<>();
        while (resultSet.next()) {
            Message msg = Message.createPrivateTextMessage(resultSet.getString("message"),
                    resultSet.getString("sender"), resultSet.getString("recipient"));

            messageList.add(msg);
        }

        resultSet.close();
        return messageList;
    }

}
