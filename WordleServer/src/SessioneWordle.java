import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

//Classe che implementa serializable in modo tale da poter salvare la sessione corrente di gioco
//ogni volta che il server viene chiuso. La serializzazione di tale struttura dati deve essere effettuata
//come ultima operazione in modo da poter poi recuperare dal file json l'oggeto
public class SessioneWordle implements Serializable {

    private String word; //parola di una sessione di gioco

    private ConcurrentHashMap<String, Integer> Tentativi;

    @JsonCreator
    public SessioneWordle(@JsonProperty("word") String w) {
        Tentativi = new ConcurrentHashMap<>();
        word = w;
    }

    //metodi get e set
    public void setWord(String w) {word = w;}
    public String getWord() {return word;}
    public boolean setTentativi(String username) {

        if(Tentativi.get(username) == null) {
            Tentativi.put(username, 1);
        }
        else {
            if(Tentativi.get(username) < 12) {
                Tentativi.put(username, Tentativi.get(username) + 1);
            }
            else return false;
        }
        return true;
    }
}
