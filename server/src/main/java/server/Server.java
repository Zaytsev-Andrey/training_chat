package server;

import log.ConsoleLogger;
import messages.Message;
import parameters.Parameter;
import server.storages.SimpleUserStorage;
import server.storages.UserStorage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Server {
    private ServerSocket serverSocket;
    private Socket socket;
    private CopyOnWriteArrayList<ClientHandler> activeClients;

    public Server() {
        try {
            activeClients = new CopyOnWriteArrayList();
            serverSocket = new ServerSocket(Parameter.PORT);
            ConsoleLogger.serverIsRunning();
            UserStorage userStorage = new SimpleUserStorage();


            while (true) {
                socket = serverSocket.accept();
                new ClientHandler(socket, this, userStorage);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void broadcastMessage(Message msg) {
        activeClients.forEach(clientHandler -> {
            clientHandler.sendMsg(msg);
        });
    }

    public void privateMessage(Message msg, ClientHandler sender) {
        for (ClientHandler client : activeClients) {
            if (client.getClientNick().equals(msg.getRecipient())) {
                client.sendMsg(msg);
                break;
            }
        }

        sender.sendMsg(msg);
    }

    public void connectClient(ClientHandler client) {
        activeClients.add(client);
        sendUserList();
    }

    public void disconnectClient(ClientHandler client) {
        activeClients.remove(client);
        sendUserList();
    }

    private void sendUserList() {
        Message message = Message.createUserListMessage(activeClients.stream()
                .map(ClientHandler::getClientNick)
                .collect(Collectors.toList()));

        broadcastMessage(message);
    }

    public boolean isClientConnected(String nick) {
        return activeClients.stream()
                .map(ClientHandler::getClientNick)
                .anyMatch(c -> c.equals(nick));
    }
}
