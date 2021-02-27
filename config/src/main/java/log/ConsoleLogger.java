package log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConsoleLogger {
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss", Locale.ENGLISH);

    public static void clientConnectedToServer(String address) {
        System.out.printf("[%s]: New client connection with address \"%s\"\n", dateFormat.format(new Date()), address);
    }

    public static void clientPassedAuth(String nick, String address) {
        System.out.printf("[%s]: The client with the address \"%s\" is authenticated with the nickname \"%s\"\n",
                dateFormat.format(new Date()), address, nick);
    }

    public static void clientDisconnectedToServer(String nick) {
        System.out.printf("[%s]: The client \"%s\" disconnected\n", dateFormat.format(new Date()), nick);
    }

    public static void clientInterruptedConnection(String nick) {
        System.out.printf("[%s]: Client connection \"%s\" interrupted\n", dateFormat.format(new Date()), nick);
    }

    public static void serverInterruptedConnection(String nick) {
        System.out.printf("[%s]: Server connection \"%s\" interrupted\n", dateFormat.format(new Date()), nick);
    }

    public static void serverIsRunning() {
        System.out.printf("[%s]: Server is running\n", dateFormat.format(new Date()));
    }
}
