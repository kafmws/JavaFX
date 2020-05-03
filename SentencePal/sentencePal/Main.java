package sentencePal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/SentencePal.fxml"));
        primaryStage.setTitle("SentencePal");
        //Main.class.getResourceAsStream("/宫内莲华.jpg")
//        primaryStage.getIcons().add(new Image(Application.class.getResourceAsStream("/宫内莲华.jpg")));
        primaryStage.setScene(new Scene(root, 768, 507));
        primaryStage.setResizable(false);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
