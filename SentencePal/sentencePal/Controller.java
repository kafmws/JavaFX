package sentencePal;

import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public ListView<String> clientList;
    public Button connectBtn;
    public Button sendBtn;
    public TextArea screen;
    public TextArea console;
    public TextField hostTextFiled;


    private Client client;
    private StringBuilder req;
    private StringBuilder consoleText;

    private BufferedInputStream in;
    private ToggleGroup group;
    private PrintWriter out;
    private Socket socket;

    public void connect() {
        nameRadio.setSelected(true);
        consoleText = new StringBuilder();
        client = new Client();
        req = client.getReq();
        try {
            socket = new Socket(hostTextFiled.getText(), 12345);
            in = new BufferedInputStream(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            screen.appendText("failed to connect " + hostTextFiled.getText());
            client.setAlive(false);
        }
        if (!client.getAlive()) return;
        client.setScreenStream(
                new Util.ScreenPrintStream(screen,
                        new ByteArrayOutputStream(10240)));
        client.setSocket(socket);
        client.setOut(out);
        client.setIn(in);
        new Thread(client).start();
        screen.appendText("connect success\ninput your name:");
    }

    public void send() {
        if (checkIsConnect()) return;
        if(console.getLength() == 0) {
            popWindowAlert("Sending content cannot be empty!");
            return;
        }
        consoleText.append(console.getText());
        Util.filterString(consoleText);
        if(consoleText.length() == 0) {
            popWindowAlert("Valid sending content cannot be empty!");
            return;
        }

        req.append(group.getSelectedToggle().getUserData())
                .append(':')
                .append(consoleText);
        out.print(req.toString());
        out.flush();
        screen.appendText(consoleText.toString());
        screen.appendText("\r\n");
        console.setText("");
        consoleText.setLength(0);
        req.setLength(0);
    }

    public Label localIP;
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
        nameRadio.setSelected(true);
    }

    private boolean checkIsConnect() {
        if (client == null || !client.getAlive()) {
            popWindowAlert("You haven't connect to server!");
            return true;
        }
        return !client.getAlive();
    }

    private void popWindowAlert(String tip){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Tip");
        alert.setHeaderText(null);
        alert.setContentText(tip);
        alert.showAndWait();
    }
}
