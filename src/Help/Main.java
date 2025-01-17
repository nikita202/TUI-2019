package Help;

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
        Parent root = FXMLLoader.load(getClass().getResource("/Help/ui.fxml"));
        primaryStage.setTitle("Help");
        primaryStage.getIcons().add(new Image("/Menu/drone.png"));
        //  root.setStyle("-fx-background-color:rgba(0, 255, 0 , 0.3)");
        primaryStage.setScene(new Scene(root, 625, 600));
        primaryStage.show();
    }


    public static void main(String[] args) throws ImageProcessingException, IOException {
        launch(args);
    }
}
