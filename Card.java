import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Card {
    public enum cardStatus { TODO, INPROGRESS, TOBEREVISED, DONE}
    private String name;
    private String description;
    private cardStatus CurrentState;
    private ArrayList<cardStatus> cardHistory;

    public Card(String name, String description){
        this.name = name;
        this.description = description;
        this.CurrentState = cardStatus.TODO;
        this.cardHistory = new ArrayList<>();
        this.cardHistory.add(cardStatus.TODO);
    }

    public Card() {}

    public String GetName() {return this.name;}
    public String GetDesc() {return this.description;}
    public String GetCList() {return this.CurrentState.name();}
    public ArrayList<String> GetCardHistory() 
    {
        ArrayList<String> lst = new ArrayList<>();
        for (cardStatus cardStatus : cardHistory) {
            lst.add(cardStatus.name());
        }
        return lst;
    }

    public void setCardHistory(ArrayList<cardStatus> cardHistory) { this.cardHistory = cardHistory; }
    public void setCurrentState(String CurrentState) { this.CurrentState = cardStatus.valueOf(CurrentState.toUpperCase());}
    public void setName(String name) {this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public String getDescription() { return description;}


    @JsonIgnore
    public ArrayList<String> getInfo()
    {
        ArrayList<String> lst = new ArrayList<>();
        lst.add(name);
        lst.add(description);
        lst.add(CurrentState.name());
        return lst;
    }

    @JsonIgnore
    public void ChangeStatus(String status)                 //Funzione che mi permette di cambiare lo stato della Card
    {
        this.CurrentState=cardStatus.valueOf(status.toUpperCase());
        this.cardHistory.add(CurrentState);
    }
}
