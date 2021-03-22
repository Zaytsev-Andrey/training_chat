package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class AppServer {
    private static final Logger LOGGER = Logger.getLogger(AppServer.class.getName());

    public static void main(String[] args) {
        LogManager manager = LogManager.getLogManager();
        try {
            manager.readConfiguration(new FileInputStream("server/logging.properties"));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading logger configuration", e);
        }

        new Server();
    }
}
