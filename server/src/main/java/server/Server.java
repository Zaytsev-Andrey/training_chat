package server;

import log.ConsoleLogger;
import messages.Message;
import parameters.ParameterApp;
import server.storages.SQLiteStorage;
import server.storages.UserStorage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Server {
    private ServerSocket serverSocket;
    private Socket socket;
    private CopyOnWriteArrayList<ClientHandler> activeClients;
    private UserStorage userStorage;
    private ExecutorService executorService;

    public Server() {
        // Для запуска клиентских соединений в отдельных потоках использовал реализацию интерфейса ExecutorService
        // создаваемую с помощью фабричного метода newCachedThreadPool() класса Executors. Данная реализация позволит
        // мгновенно запускать новую задачу (используя существующий поток или создавая новый). Заметное увеличение
        // прозводительности будет при интенсивном отключении и подключение новых клиентов в интервале 60 сек, когда
        // освоодившиеся потоки не будут уничтожаться и смогут принимать новые задачи. Поскольку интенсивность подключий
        // клиентов может быть не равномерной (в одно время суток достигать максимума, а в другое клиентских подключний
        // может и не быть), реализация newCachedThreadPool позволит удалить не используемые потоки и освободить ресурсы.
        // Большим недостатком такой реализации является фактически не ограниченное (Integer.MAX_VALUE) число возможных
        // потоков, что может привести к перерасходу ресурсов.
        //
        // Фабричные методы класса Executors возвращают объекты класса ThreadPoolExecutor с фиксированными
        // значениями конструктора, например для newCachedThreadPool: new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L,
        // TimeUnit.SECONDS, new SynchronousQueue<Runnable>()) где вторым параметром передается максимально число потоков,
        // а третьим параметром время простоя потока перед уничтожением. В случае обнаружения проблем в работе текущей
        // реализации ExecutorService возможно создать свой ThreadPoolExecutor настроив его более точно (ограничив
        // максимальное число потоков или увеличив время простоя потока перед уничтожением). При этом остальной код
        // программы останется неизменным.
        executorService = Executors.newCachedThreadPool();

        try {
            userStorage = new SQLiteStorage();
            activeClients = new CopyOnWriteArrayList();
            serverSocket = new ServerSocket(ParameterApp.PORT);

            ConsoleLogger.serverIsRunning();

            while (true) {
                socket = serverSocket.accept();
                ClientHandler newClient = new ClientHandler(socket, this, userStorage);
                executorService.execute(newClient);
            }

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();

            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (userStorage != null) {
                userStorage.disconnected();
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

    public void sendUserList() {
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
