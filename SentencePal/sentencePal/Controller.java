package sentencePal;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    public Button sendBtn;
    public Button connectBtn;
    public TextArea screen;
    public TextArea console;
    public TextField hostTextFiled;
    public ListView<String> clientListView;
    private ObservableList<String> clientList;


    private Client client;
    private StringBuilder req;
    private StringBuilder consoleText;

    private BufferedInputStream in;
    private ToggleGroup group;
    private PrintWriter out;
    private Socket socket;

    public void connect() {
        if (socket != null) {
            popWindowAlert("You already own a connection!");
            return;
        }

        mainPane.getScene().getWindow().setOnCloseRequest(windowEvent -> {
            if(client.getAlive()){
                out.print("exit: ");
                out.flush();
                client.setAlive(false);
            }
            System.exit(0);
        });

        client = new Client();
        req = client.getReq();
        consoleText = new StringBuilder();
        clientList = FXCollections.observableList(new ArrayList<>());

        try {
            socket = new Socket(hostTextFiled.getText(), 12345);
            in = new BufferedInputStream(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            screen.appendText("failed to connect " + hostTextFiled.getText() + "\r\n");
            client.setAlive(false);
            socket = null;
        }
        if (!client.getAlive()) return;
        client.setScreenStream(
                new Util.ScreenPrintStream(screen,
                        new ByteArrayOutputStream(10240)));
        client.setClientList(clientList);
        client.setSocket(socket);
        client.setOut(out);
        client.setIn(in);
        new Thread(client).start();
        screen.appendText("connect success\ninput your name:");
        console.requestFocus();
        nameRadio.setSelected(true);
        clientListView.setItems(clientList);

    }

    public void send() {
        if (checkIsConnect()) return;
        if (!nameRadio.isSelected() && client.getUsername().equals("")) {
            popWindowAlert("Please get a valid name first!");
            nameRadio.setSelected(true);
            console.setText("");
            return;
        }
        if (!todayRadio.isSelected()) {
            if (console.getLength() == 0) {
                popWindowAlert("Sending content cannot be empty!");
                return;
            }
            consoleText.append(console.getText());
            Util.filterString(consoleText);
            if (consoleText.length() == 0) {
                popWindowAlert("Valid sending content cannot be empty!");
                return;
            }

            req.append(group.getSelectedToggle().getUserData())
                    .append(':')
                    .append(consoleText);

            screen.appendText(consoleText.toString());
            screen.appendText("\r\n");
        }
        out.print(req.toString());
        out.flush();
        console.setText("");
        consoleText.setLength(0);
        req.setLength(0);
    }

    public Label localIP;
    public Pane mainPane;
    public RadioButton sentenceRadio;
    public RadioButton uploadRadio;
    public RadioButton todayRadio;
    public RadioButton shareRadio;
    public RadioButton nameRadio;
    public RadioButton talkRadio;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        localIP.setText(Util.getIPAddress());

        group = new ToggleGroup();
        sentenceRadio.setToggleGroup(group);
        uploadRadio.setToggleGroup(group);
        todayRadio.setToggleGroup(group);
        shareRadio.setToggleGroup(group);
        talkRadio.setToggleGroup(group);
        nameRadio.setToggleGroup(group);
        sentenceRadio.setUserData("sentence");
        uploadRadio.setUserData("upload");
        todayRadio.setUserData("today");
        shareRadio.setUserData("share");
        talkRadio.setUserData("talk");
        nameRadio.setUserData("name");
        group.selectedToggleProperty()
                .addListener((ObservableValue<? extends Toggle> ov, Toggle old_toggle,
                              Toggle new_toggle) -> {
                    if (old_toggle != null) old_toggle.setSelected(false);
                });
        todayRadio.setOnMouseClicked(mouseEvent -> {
            req.append("today:motto");
            send();
        });
        nameRadio.setSelected(true);

        EventHandler<KeyEvent> defaultSend = keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                keyEvent.consume();
                send();
            }
        };
        sendBtn.setOnKeyPressed(defaultSend);
        console.setOnKeyPressed(defaultSend);
    }

    private boolean checkIsConnect() {
        if (client == null || !client.getAlive()) {
            popWindowAlert("You haven't connect to server!");
            return true;
        }
        return !client.getAlive();
    }

    private void popWindowAlert(String tip) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Tip");
        alert.setHeaderText(null);
        alert.setContentText(tip);
        alert.showAndWait();
    }
}
