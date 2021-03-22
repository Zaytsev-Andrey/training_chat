package server;

import comands.ReasonAuthExceptions;
import comands.SessionStatus;
import messages.Message;
import messages.MessageType;
import server.exceptions.AuthExceptions;
import server.storages.MessageStorage;
import server.storages.UserStorage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private SessionStatus status;
    private Server server;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private UserStorage userStorage;
    private MessageStorage messageStorage;
    private String clientNick;


    public ClientHandler(Socket socket, Server server, UserStorage userStorage) {
        this.server = server;
        this.socket = socket;
        this.userStorage = userStorage;
        this.messageStorage = (MessageStorage) userStorage;

        status = SessionStatus.NOT_AUTH;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            String infoMsg = String.format("New connection from address %s", socket.getInetAddress().toString());
            LOGGER.info(infoMsg);
            socket.setSoTimeout(120000);

            auth();
            readMessage();
        } catch (SocketTimeoutException e) {
            Message msg = Message.createEndMessage();
            sendMsg(msg);
            status = SessionStatus.DISCONNECTED;
            String infoMsg = String.format("Connection timeout for address %s", socket.getInetAddress().toString());
            LOGGER.info(infoMsg);
        } catch (IOException e) {
            String errorMsg = String.format("Client connection \"%s\" is interrupted", clientNick);
            LOGGER.log(Level.SEVERE, errorMsg, e);
        }
        finally {
            server.disconnectClient(this);

            try {
                in.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Output stream error", e);
            }
            try {
                out.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Input stream error", e);
            }
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Client socket error", e);
            }
        }
    }

    private void auth() throws IOException {
        Message msg;


        while (status == SessionStatus.NOT_AUTH) {
            try {
                msg = (Message) in.readObject();
                String commandMsg = String.format("Client %s sent command %s",
                        socket.getInetAddress().toString(), msg.getMessageType().toString());
                LOGGER.config(commandMsg);

                if (msg.getMessageType() == MessageType.END) {
                    sendMsg(msg);
                    String infoMsg = String.format("Client %s disconnected", socket.getInetAddress().toString());
                    LOGGER.info(infoMsg);
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
                        String infoMsg = String.format("Client %s connected", clientNick);
                        LOGGER.info(infoMsg);
                    } catch (AuthExceptions e) {
                        answerMsg = Message.createAuthFailMessage(e.getReason());
                        sendMsg(answerMsg);
                        String errorMsg = String.format("Authentication error from address %s. %s",
                                socket.getInetAddress().toString(), e.getReason());
                        LOGGER.warning(errorMsg);
                    }
                }

                if (msg.getMessageType() == MessageType.REG) {
                    Message answerMsg;
                    try {
                        userStorage.add(msg.getLogin(), msg.getPassword(), msg.getNick());
                        answerMsg = Message.createRegOkMessage();
                    } catch (AuthExceptions e) {
                        answerMsg = Message.createRegFailMessage(e.getReason());
                        String errorMsg = String.format("Registration error with login \"%s\" and nickname \"%s\". %s",
                                msg.getLogin(), msg.getNick(), e.getReason());
                        LOGGER.warning(errorMsg);
                    }

                    sendMsg(answerMsg);
                }

            } catch (ClassNotFoundException e) {
                String errorMsg = String.format("Invalid message format from client %s", clientNick);
                LOGGER.log(Level.WARNING, errorMsg, e);
            }
        }
    }

    private void readMessage() throws IOException {
        Message msg;

        while (status == SessionStatus.CONNECTED) {
            try {
                msg = (Message) in.readObject();
                String commandMsg = String.format("Client %s sent command %s",
                        clientNick, msg.getMessageType().toString());
                LOGGER.config(commandMsg);

                if (msg.getMessageType() == MessageType.END) {
                    sendMsg(msg);
                    String infoMsg = String.format("Client %s disconnected", clientNick);
                    LOGGER.info(infoMsg);
                    status = SessionStatus.DISCONNECTED;
                }

                if (msg.getMessageType() == MessageType.CHANGE_NICK) {
                    Message answerMsg;
                    try {
                        userStorage.changeNick(clientNick, msg.getNewNick());
                        clientNick = msg.getNewNick();
                        answerMsg = Message.createChangeNickOkMessage(msg.getNewNick());
                        sendMsg(answerMsg);
                        server.sendUserList();
                    } catch (AuthExceptions e) {
                        answerMsg = Message.createChangeNickFailMessage(e.getReason());
                        sendMsg(answerMsg);
                        String errorMsg = String.format("Error changing nickname \"%s\" to \"%s\". %s",
                                clientNick, msg.getNewNick(), e.getReason());
                        LOGGER.warning(errorMsg);
                    }
                }

                if (msg.getMessageType() == MessageType.GET_TEXT) {
                    try {
                        List<Message> messageList = messageStorage.getMessageList(clientNick);
                        Message answerMsg = Message.createTextListMessage(messageList);
                        sendMsg(answerMsg);
                    } catch (SQLException e) {
                        String errorMsg = String.format("Error getting message list for client \"%s\"", clientNick);
                        LOGGER.log(Level.WARNING, errorMsg, e);
                    }
                }

                if (msg.getMessageType() == MessageType.TEXT) {
                    try {
                        messageStorage.addMessage(clientNick, msg.getRecipient(), msg.getText());
                        server.broadcastMessage(msg);
                    } catch (SQLException e) {
                        String errorMsg = String.format("Error adding message \"%s\" to database", msg.getText());
                        LOGGER.log(Level.WARNING, errorMsg, e);
                    }
                }

                if (msg.getMessageType() == MessageType.PRIVATE_TEXT) {
                    try {
                        messageStorage.addMessage(clientNick, msg.getRecipient(), msg.getText());
                        server.privateMessage(msg, this);
                    } catch (SQLException e) {
                        String errorMsg = String.format("Error adding message \"%s\" to database", msg.getText());
                        LOGGER.log(Level.WARNING, errorMsg, e);
                    }
                }
            } catch (ClassNotFoundException e) {
                String errorMsg = String.format("Invalid message format from client %s", clientNick);
                LOGGER.log(Level.WARNING, errorMsg, e);
            }
        }
    }

    public void sendMsg(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            String errorMsg = String.format("Error sending message \"%s\" to client \"%s\"", msg.getText(), clientNick);
            LOGGER.log(Level.WARNING, errorMsg, e);
        }
    }

    public String getClientNick() {
        return clientNick;
    }



}
