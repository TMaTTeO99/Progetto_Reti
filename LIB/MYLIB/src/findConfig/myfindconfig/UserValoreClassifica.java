import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class UserValoreClassifica implements Comparable<UserValoreClassifica>, Serializable {

    private String username;
    private float score;

    @JsonCreator
    public UserValoreClassifica(@JsonProperty("username") String us, @JsonProperty("score") float sc) {
        username = us;
        score = sc;
    }

    public float getScore() {return score;}
    public void setScore(float sc) {score = sc;}
    public void setUsername(String usn) {username = usn;}
    public String getUsername() {return username;}

    @Override //per decidere l ordine se crescente o decrrescente, bisogna cambiarre questo metodo
    public int compareTo(UserValoreClassifica o) {return Float.compare(o.getScore(), score);}

    //Metodo per aggiornare direttamente dalloggetto lo score del giocatore
    public void UpdateSCore(int Wingame, int tentativi) {score = (1 / (float)((float) tentativi / (float) Wingame)) * Wingame;}
}
