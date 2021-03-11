package client;

import comands.SessionStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import log.ConsoleLogger;
import messages.Message;
import messages.MessageType;
import parameters.ParameterApp;
import parameters.ParameterBD;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    @FXML
    private TextArea chatText;
    @FXML
    private TextField messageText;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private MenuItem menuLogOf;
    @FXML
    private MenuItem menuChangeNick;
    @FXML
    private VBox authPane;
    @FXML
    private VBox chatPane;
    @FXML
    private Label wrongAuthMessage;
    @FXML
    private ListView<String> activeUsers;
    @FXML
    private Button recipient;
    @FXML
    private Button btnLogin;

    private Stage stage;
    private Stage regStage;
    private RegController regController;
    private String programName;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private SessionStatus status;
    private String clientNick;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        status = SessionStatus.DISCONNECTED;

        Platform.runLater(() -> {
            stage = (Stage) chatText.getScene().getWindow();


            programName = stage.getTitle();
            btnLogin.requestFocus();

            stage.setOnCloseRequest(event -> {
                if (status != SessionStatus.DISCONNECTED) {
                    logOff();
                }
            });
        });
    }

    private void connect() {
        try {
            socket = new Socket(ParameterApp.IP_ADDRESS, ParameterApp.PORT);
            ConsoleLogger.clientConnectedToServer(socket.getInetAddress().toString());
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            status = SessionStatus.NOT_AUTH;

            new Thread(() -> {
                try {
                    auth();
                    readMessage();
                } catch (IOException e) {
                    ConsoleLogger.serverInterruptedConnection(clientNick);
                } finally {
                    status = SessionStatus.DISCONNECTED;
                    clientNick = null;
                    switchInterface();

                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        in.close();
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

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void auth() throws IOException {
        while (status == SessionStatus.NOT_AUTH) {
            try {
                Message message = (Message) in.readObject();

                if (message.getMessageType() == MessageType.END) {
                    ConsoleLogger.clientDisconnectedToServer(clientNick);
                    status = SessionStatus.DISCONNECTED;
                }

                if (message.getMessageType() == MessageType.AUTH_OK) {
                    clientNick = message.getNick();
                    status = SessionStatus.CONNECTED;
                    clientNick = message.getNick();
                    switchInterface();
                    requestHistoryOfMessages();
                    ConsoleLogger.clientPassedAuth(clientNick, socket.getInetAddress().toString());
                }

                if (message.getMessageType() == MessageType.AUTH_FAIL) {
                    Platform.runLater(() -> {
                        wrongAuthMessage.setText(message.getReasonMessage());
                        loginField.clear();
                        passwordField.clear();
                    });
                }

                if (message.getMessageType() == MessageType.REG_OK || message.getMessageType() == MessageType.REG_FAIL) {
                    regController.showRegMessage(message);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void readMessage() throws IOException {

        while (status == SessionStatus.CONNECTED) {
            try {
                Message message = (Message) in.readObject();

                if (message.getMessageType() == MessageType.END) {
                    status = SessionStatus.DISCONNECTED;
                    ConsoleLogger.clientDisconnectedToServer(clientNick);
                }

                if (message.getMessageType() == MessageType.CHANGE_NICK_OK) {
                    clientNick = message.getNick();
                    setTitle();
                }

                if (message.getMessageType() == MessageType.CHANGE_NICK_FAIL) {
                    changeNickFailMessage(message.getReasonMessage());
                }

                if (message.getMessageType() == MessageType.TEXT_LIST) {
                    loadHistoryOfMessages(message);
                }

                if (message.getMessageType() == MessageType.TEXT) {
                    String text = String.format("[%s]: %s\n", message.getSender(), message.getText());
                    chatText.appendText(text);
                }

                if (message.getMessageType() == MessageType.PRIVATE_TEXT) {
                    String text = String.format("[%s] for [%s]: %s\n", message.getSender(), message.getRecipient(), message.getText());
                    chatText.appendText(text);
                }

                if (message.getMessageType() == MessageType.USER_LIST) {
                    List<String> list = message.getActiveUser().stream()
                            .filter(s -> !s.equals(clientNick))
                            .collect(Collectors.toList());
                    list.add(0, ParameterBD.ALL_USER_NICK);

                    Platform.runLater(() -> {
                        activeUsers.getItems().clear();
                        activeUsers.getItems().addAll(list);
                    });
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMsg(Message message) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logOffAction(ActionEvent actionEvent) {
        logOff();
    }

    private void logOff() {
        sendMsg(Message.createEndMessage());
    }

    public void loginAction(ActionEvent actionEvent) {
        String login = loginField.getText().toLowerCase().trim();
        String pass = passwordField.getText().trim();

        Message message = Message.createAuthMessage(login, pass);
        sendMsg(message);
    }

    private void switchInterface() {
        Platform.runLater(() -> {
            menuChangeNick.setDisable(!status.isState());
            menuLogOf.setDisable(!status.isState());
            authPane.setVisible(!status.isState());
            authPane.setManaged(!status.isState());
            chatPane.setVisible(status.isState());
            chatPane.setManaged(status.isState());

            wrongAuthMessage.setText("");
            passwordField.clear();
            chatText.clear();
            stage.requestFocus();
            recipient.setText(ParameterBD.ALL_USER_NICK);
        });

        setTitle();
    }

    private void setTitle() {
        String title;
        if (clientNick != null) {
            title = String.format("%s - [%s]", programName, clientNick);
        } else {
            title = programName;
        }
        Platform.runLater(() -> stage.setTitle(title));

    }

    public void regWindowAction(ActionEvent actionEvent) {
        if (regStage == null) {
            initRegStage();
        }

        Platform.runLater(() -> regStage.show());
    }

    private void initRegStage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();

            regController = fxmlLoader.getController();
            regController.setMainController(this);

            regStage = new Stage();
            regStage.initStyle(StageStyle.UTILITY);
            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.setResizable(false);
            regStage.setTitle(programName + " - Registration");
            regStage.setScene(new Scene(root, 250, 220));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recipientAction(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            activeUsers.setVisible(true);
            activeUsers.setManaged(true);
        });
    }

    public void chooseAction(MouseEvent mouseEvent) {
        Platform.runLater(() -> {
            int index = activeUsers.getSelectionModel().getSelectedIndex();

            if (index >= 0) {
                recipient.setText(activeUsers.getItems().get(index));
            }

            activeUsers.setVisible(false);
            activeUsers.setManaged(false);
        });
    }

    public void sendAction(ActionEvent actionEvent) {
        String text = messageText.getText();
        Message message;
        if (!text.isEmpty()) {
            if (recipient.getText().equals("All")) {
                message = Message.createTextMessage(clientNick, text);
            } else {
                message = Message.createPrivateTextMessage(text, clientNick, recipient.getText());
            }

            sendMsg(message);
        }

        messageText.clear();
        messageText.requestFocus();
    }

    public void changeNick(ActionEvent actionEvent) {
        TextInputDialog inputDialog = new TextInputDialog(clientNick);
        inputDialog.setTitle("Change Nick");
        inputDialog.setHeaderText("Input new nick:");
        inputDialog.getEditor().setPrefWidth(300.0);
        Optional<String> newNick = inputDialog.showAndWait();

        if (newNick.isPresent() && !newNick.get().equals(clientNick)) {
            Message msg = Message.createChangeNickMessage(newNick.get());
            sendMsg(msg);
        }
    }

    private void changeNickFailMessage(String reasonMessage) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, reasonMessage);
            alert.setTitle("Change Nick");
            alert.showAndWait();
        });

    }

    private void loadHistoryOfMessages(Message message) {
        String text;
        StringBuilder builder = new StringBuilder();
        for (Message msg : message.getMessageList()) {
            if (msg.getRecipient().equals(ParameterBD.ALL_USER_NICK)) {
                text = String.format("[%s]: %s\n", msg.getSender(), msg.getText());
            } else {
                text = String.format("[%s] for [%s]: %s\n", msg.getSender(), msg.getRecipient(), msg.getText());
            }

            builder.append(text);
        }

        chatText.appendText(builder.toString());
    }

    private void requestHistoryOfMessages() {
        Message msg = Message.createGetTextMessage(clientNick);
        sendMsg(msg);
    }
}
