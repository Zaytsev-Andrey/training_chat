package client;

import comands.ReasonAuthExceptions;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import messages.Message;
import messages.MessageType;

import java.net.URL;
import java.util.ResourceBundle;

public class RegController implements Initializable {
    @FXML
    private TextField regLoginField;
    @FXML
    private TextField regPasswordField;
    @FXML
    private TextField regNickField;
    @FXML
    private Label regWrongAuthMessage;
    @FXML
    private Label regWrongLoginMessage;
    @FXML
    private Label regWrongPassMessage;
    @FXML
    private Label regWrongNickMessage;
    @FXML
    private Button btnReg;

    private Controller mainController;

    public void setMainController(Controller mainController) {
        this.mainController = mainController;
    }

    public void regAction(ActionEvent actionEvent) {
        String login = regLoginField.getText().trim();
        String password = regPasswordField.getText();
        String nick = regNickField.getText().trim();

        if (!login.isEmpty() && !password.isEmpty() && !nick.isEmpty()) {
            Message message = Message.createRegMessage(login, password, nick);
            mainController.sendMsg(message);
        }
    }

    public void showRegMessage(Message message) {
        if (message.getMessageType() == MessageType.REG_OK) {
            Platform.runLater(() -> {
                regLoginField.clear();
                regPasswordField.clear();
                regNickField.clear();

                regWrongAuthMessage.setText("Successful registration");
            });
        }

        if (message.getMessageType() == MessageType.REG_FAIL) {
            Platform.runLater(() -> {
                if (message.getReason() == ReasonAuthExceptions.LOGIN_AND_NICK_EXIST) {
                    regLoginField.clear();
                    regNickField.clear();
                } else if (message.getReason() == ReasonAuthExceptions.LOGIN_EXIST) {
                    regLoginField.clear();
                } else if (message.getReason() == ReasonAuthExceptions.NICK_EXIST) {
                    regNickField.clear();
                }

                regWrongAuthMessage.setText(message.getReasonMessage());
            });
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> btnReg.requestFocus());
    }
}
