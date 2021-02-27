package server.exceptions;

import comands.ReasonAuthExceptions;

public class AuthExceptions extends RuntimeException {
    private final ReasonAuthExceptions reason;

    public AuthExceptions(ReasonAuthExceptions reason) {
        this.reason = reason;
    }

    public ReasonAuthExceptions getReason() {
        return reason;
    }
}
