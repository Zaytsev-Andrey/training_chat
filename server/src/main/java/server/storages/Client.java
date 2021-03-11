package server.storages;

public class Client {
    private int id;
    private String login;
    private String password;
    private String nick;

    public Client(int id, String nick) {
        this.id = id;
        this.nick = nick;
    }

    public int getId() {
        return id;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }
}
