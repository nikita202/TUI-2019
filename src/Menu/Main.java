package Menu;

import com.drew.imaging.ImageProcessingException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/Menu/ui.fxml"));
        primaryStage.setTitle("Menu");
        primaryStage.getIcons().add(new Image("/resources/drone.png"));
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }


    public static void main(String[] args) throws ImageProcessingException, IOException {
        launch(args);
    }
}
