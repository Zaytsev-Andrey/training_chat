package server.storages;

import comands.ReasonAuthExceptions;
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
    private PreparedStatement prStmtGetId;
    private PreparedStatement prStmtGetNick;
    private PreparedStatement prStmtAddMsg;
    private PreparedStatement prStmtGetMsg;

    public SQLiteStorage() throws ClassNotFoundException, SQLException {
        connection();
        prStmtLogin = connection.prepareStatement("SELECT id, nick FROM users WHERE login = ? AND password = ?;");
        prStmtRegister = connection.prepareStatement("INSERT INTO users (login, password, nick) VALUES (?, ?, ?);");
        prStmtUpdateNick = connection.prepareStatement("UPDATE users SET nick = ? WHERE id = ?;");
        prStmtGetId = connection.prepareStatement("SELECT id FROM users WHERE nick = ?;");
        prStmtGetNick = connection.prepareStatement("SELECT nick FROM users WHERE id = ?;");
        prStmtAddMsg = connection.prepareStatement("INSERT INTO messages (sender_id, recipient_id, message) VALUES (?, ?, ?);");
        prStmtGetMsg = connection.prepareStatement("SELECT sender_id, recipient_id, message FROM messages " +
                "WHERE sender_id = ? OR recipient_id = ? OR recipient_id = ?;");

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
    public Client login(String login, String password) {
        ResultSet resultSet = null;
        Client client;
        try {
            prStmtLogin.setString(1, login.toLowerCase());
            prStmtLogin.setString(2, password);
            resultSet = prStmtLogin.executeQuery();

            if (resultSet.next()) {
                client = new Client(resultSet.getInt("id"), resultSet.getString("nick"));
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

        return client;
    }

    @Override
    public void changeNick(int id, String newNick) {
        try {
            prStmtUpdateNick.setString(1, newNick.toLowerCase());
            prStmtUpdateNick.setInt(2, id);
            prStmtUpdateNick.executeUpdate();
        } catch (SQLException e) {
            throw new AuthExceptions(ReasonAuthExceptions.NICK_EXIST);
        }
    }

    @Override
    public int getIdForNick(String nick) throws SQLException {

        prStmtGetId.setString(1, nick);
        ResultSet resultSet = prStmtGetId.executeQuery();

        if (resultSet.next()) {
            int id = resultSet.getInt("id");
            resultSet.close();
            return id;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void addMessage(int sender, int recipient, String message) throws SQLException {
        prStmtAddMsg.setInt(1, sender);
        prStmtAddMsg.setInt(2, recipient);
        prStmtAddMsg.setString(3, message);
        prStmtAddMsg.executeUpdate();
    }

    @Override
    public List<String[]> getMessage(int sender) throws SQLException {
        int allId = getIdForNick("All");
        prStmtGetMsg.setInt(1, sender);
        prStmtGetMsg.setInt(2, sender);
        prStmtGetMsg.setInt(3, allId);
        ResultSet resultSet = prStmtGetMsg.executeQuery();

        List<String[]> messageList = new ArrayList<>();
        while (resultSet.next()) {
            String[] arr = new String[3];

            arr[0] = getNickForId(resultSet.getInt("sender_id"));
            arr[1] = getNickForId(resultSet.getInt("recipient_id"));
            arr[2] = resultSet.getString("message");

            messageList.add(arr);
        }

        resultSet.close();
        return messageList;
    }

    private String getNickForId(int id) throws SQLException {
        prStmtGetNick.setInt(1, id);
        ResultSet resultSet = prStmtGetNick.executeQuery();

        if (resultSet.next()) {
            String nick = resultSet.getString("nick");
            resultSet.close();
            return nick;
        } else {
            throw new IllegalArgumentException();
        }
    }
}
