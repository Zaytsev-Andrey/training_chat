package comands;

public enum SessionStatus {
    NOT_AUTH (false),
    CONNECTED (true),
    DISCONNECTED (false);

    private boolean state;

    SessionStatus(boolean state) {
        this.state = state;
    }

    public boolean isState() {
        return state;
    }
}
