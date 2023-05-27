import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

//Classe che implementa serializable in modo tale da poter salvare la sessione corrente di gioco
//ogni volta che il server viene chiuso. La serializzazione di tale struttura dati deve essere effettuata
//come ultima operazione in modo da poter poi recuperare dal file json l'oggeto
public class SessioneWordle implements Serializable {

    private String word; //parola di una sessione di gioco
    private long lastTime;//variabile per tenere traccia del tempo dell ultima volta che è stata scelta una parola per il gioco
    private long currentTime;//variabile per tenere traccia del tempo della parola corrente del gioco
    private long nextTime;//variabile che indica dopo quanto tempo verra creata la nuova parola
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
    public int Tentativo(String username) {

        InfoSessioneUtente info = Tentativi.get(username);

        if(info == null)return -1;//caso in cui l utente non ha selezionato il comando playWORDLE
        if(info.getTentativi() < 12 && !info.getResult()) {
            info.increaseTentativi();
            return 0;
        }
        if(info.getTentativi() >= 12) return -2;//caso in cui l utente ha gia giocato e ha terminato i tentativi

        return -3; //ha vinto la partita precedentemente
    }
    public void setWinner(String UserName) {
        InfoSessioneUtente tmp = Tentativi.get(UserName);
        tmp.setResultGame(true);
    }
    public void setLastTime(long time) {lastTime = time;}
    public long getLastTime() {return lastTime;}
    public void setCurrentTime(long Time) {currentTime = Time;}
    public long getCurrentTime() {return currentTime;}
    public void setNextTime(long Time) {nextTime = Time;}
    public long getNextTime() {return nextTime;}
}