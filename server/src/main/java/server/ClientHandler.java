package server;

import comands.ReasonAuthExceptions;
import comands.SessionStatus;
import log.ConsoleLogger;
import messages.Message;
import messages.MessageType;
import server.exceptions.AuthExceptions;
import server.storages.UserStorage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler {
    private SessionStatus status;
    private Server server;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private UserStorage userStorage;
    private String clientNick;


    public ClientHandler(Socket socket, Server server, UserStorage userStorage) {
        this.server = server;
        this.socket = socket;
        this.userStorage = userStorage;

        status = SessionStatus.NOT_AUTH;

        new Thread(() -> {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                ConsoleLogger.clientConnectedToServer(socket.getInetAddress().toString());
                socket.setSoTimeout(5000);

                auth();
                readMessage();
            } catch (SocketTimeoutException e) {
                Message msg = Message.createEndMessage();
                sendMsg(msg);
                status = SessionStatus.DISCONNECTED;
                ConsoleLogger.authorizationTimedOut(socket.getInetAddress().toString());
            } catch (IOException e) {
                ConsoleLogger.clientInterruptedConnection(clientNick);
            }
            finally {
                server.disconnectClient(this);

                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    private void auth() throws IOException {
        Message msg;

        while (status == SessionStatus.NOT_AUTH) {
            try {
                msg = (Message) in.readObject();

                if (msg.getMessageType() == MessageType.END) {
                    sendMsg(msg);
                    ConsoleLogger.clientDisconnectedToServer(clientNick);
                    status = SessionStatus.DISCONNECTED;
                }

                if (msg.getMessageType() == MessageType.AUTH) {
                    Message answerMsg;

                    try {
                        clientNick = userStorage.login(msg.getLogin(), msg.getPassword());
                        if (server.isClientConnected(clientNick)) {
                            answerMsg = Message.createAuthFailMessage(ReasonAuthExceptions.CLIENT_IS_ALREADY_CONNECTED);
                            sendMsg(answerMsg);
                            continue;
                        }

                        answerMsg = Message.createAuthOkMessage(clientNick);
                        status = SessionStatus.CONNECTED;
                        sendMsg(answerMsg);
                        server.connectClient(this);
                        socket.setSoTimeout(0);
                        ConsoleLogger.clientPassedAuth(clientNick, socket.getInetAddress().toString());
                    } catch (AuthExceptions e) {
                        answerMsg = Message.createAuthFailMessage(e.getReason());
                        sendMsg(answerMsg);
                    }
                }

                if (msg.getMessageType() == MessageType.REG) {
                    Message answerMsg;
                    try {
                        userStorage.add(msg.getLogin(), msg.getPassword(), msg.getNick());
                        answerMsg = Message.createRegOkMessage();
                    } catch (AuthExceptions e) {
                        answerMsg = Message.createRegFailMessage(e.getReason());
                    }

                    sendMsg(answerMsg);
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void readMessage() throws IOException {
        Message msg;

        while (status == SessionStatus.CONNECTED) {
            try {
                msg = (Message) in.readObject();

                if (msg.getMessageType() == MessageType.END) {
                    sendMsg(msg);
                    ConsoleLogger.clientDisconnectedToServer(clientNick);
                    status = SessionStatus.DISCONNECTED;
                }

                if (msg.getMessageType() == MessageType.TEXT) {
                    server.broadcastMessage(msg);
                }

                if (msg.getMessageType() == MessageType.PRIVATE_TEXT) {
                    server.privateMessage(msg, this);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMsg(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getClientNick() {
        return clientNick;
    }

}
