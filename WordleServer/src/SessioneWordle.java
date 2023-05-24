import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

//Classe che implementa serializable in modo tale da poter salvare la sessione corrente di gioco
//ogni volta che il server viene chiuso. La serializzazione di tale struttura dati deve essere effettuata
//come ultima operazione in modo da poter poi recuperare dal file json l'oggeto
public class SessioneWordle implements Serializable {

    private String word; //parola di una sessione di gioco

    private ConcurrentHashMap<String, InfoSessioneUtente> Tentativi;

    @JsonCreator
    public SessioneWordle() {Tentativi = new ConcurrentHashMap<>();}

    //metodi get e set
    public void setWord(String w) {word = w;}
    public String getWord() {return word;}
    public int setGame(String username) {

        if(Tentativi.get(username) == null) {
            Tentativi.put(username, new InfoSessioneUtente(0, false));
        }
        else {
            InfoSessioneUtente infoU = Tentativi.get(username);
            if(infoU.getTentativi() < 12 && !infoU.getResult()) {return 1;}//1 indica che l utente ha gia inviato la richiesta di playWORDLE
            else {return -1;}//-1 indica che l utente ha gia partecipato al gioco
        }
        return 0;//0 indica richiesta di playWORDLE andata a buon fine
    }
    public void setTentativi() {Tentativi = new ConcurrentHashMap<>();}
}