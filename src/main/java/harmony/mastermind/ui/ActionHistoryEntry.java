package harmony.mastermind.ui;

import java.util.Date;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;

//@@author A0138862W
public class ActionHistoryEntry extends UiPart{

    private static final String FXML = "ActionHistoryEntry.fxml";
    
    @FXML
    private HBox actionHistoryEntry;
    
    @FXML
    private Label title;
    
    @FXML
    private Label date;
    
    public void setTitle(String title){
        this.title.setText(title);
    }
    
    public void setDate(String date){
        this.date.setText(date);
    }

    public Node getNode(){
        return actionHistoryEntry;
    }
    
    public void setTypeFail(){
        this.title.setStyle("-fx-text-fill: -fx-quaternary;");
        this.date.setStyle("-fx-text-fill: -fx-quaternary;");
    }
    
    public void setTypeSuccess(){
        this.title.setStyle("-fx-text-fill: -fx-primary;");
        this.date.setStyle("-fx-text-fill: -fx-primary;");
    }
    
    @Override
    public void setNode(Node node) {
        actionHistoryEntry = (HBox) node;
    }

    @Override
    public String getFxmlPath() {
        return FXML;
    }
      
    

}

//@@author A0138862W
class ActionHistory {
    private final String title;
    private final String description;
    private final Date dateActioned;
    
    public ActionHistory(String title, String description){
        this.title = title;
        this.description = description;
        this.dateActioned = new Date();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Date getDateActioned() {
        return dateActioned;
    }
}
