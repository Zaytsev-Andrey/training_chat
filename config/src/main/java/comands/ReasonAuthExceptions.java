package comands;

public enum ReasonAuthExceptions {
    LOGIN_EXIST ("Login exist"),
    NICK_EXIST ("Nick exist"),
    LOGIN_AND_NICK_EXIST ("Login and nick exist"),
    INCORRECT_LOGIN_OR_PASS ("Incorrect login or password"),
    CLIENT_IS_ALREADY_CONNECTED ("Client is already connected");

    private String message;

    ReasonAuthExceptions(String messge) {
        this.message = messge;
    }

    public String getMessage() {
        return message;
    }
}
