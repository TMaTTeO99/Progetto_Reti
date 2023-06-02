import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

//Classe che implementa serializable in modo tale da poter salvare la sessione corrente di gioco
//ogni volta che il server viene chiuso. La serializzazione di tale struttura dati deve essere effettuata
//come ultima operazione in modo da poter poi recuperare dal file json l'oggeto
public class SessioneWordle implements Serializable {

    private String word; //parola di una sessione di gioco
    private String translatedWord; //traduzione della parola
    private long currentTime;//variabile per tenere traccia del tempo della parola corrente del gioco
    private long nextTime;//variabile che indica dopo quanto tempo verra creata la nuova parola

    private ConcurrentHashMap<String, InfoSessioneUtente> Tentativi;//dovro cambiare nome alla variabile

    @JsonCreator
    public SessioneWordle() {Tentativi = new ConcurrentHashMap<>();}

    public int setGame(String username) {

        InfoSessioneUtente tmpInfo = Tentativi.get(username);
        if(tmpInfo == null) {
            Tentativi.put(username, new InfoSessioneUtente(0, false));
        }
        else {
            if(tmpInfo.getTentativi() < 12 && !tmpInfo.getResultGame() && !tmpInfo.isQuitGame()) {return 1;}//1 indica che l utente ha gia inviato la richiesta di playWORDLE
            else {return -1;}//-1 indica che l utente ha gia partecipato al gioco
        }
        return 0;//0 indica richiesta di playWORDLE andata a buon fine
    }
    public int Tentativo(String username) {

        InfoSessioneUtente info = Tentativi.get(username);

        if(info == null)return -1;//caso in cui l utente non ha selezionato il comando playWORDLE
        if(info.isQuitGame())return -2;//caso in cui l utente ha gia giocato e ha effettuato il logouti
        if(info.getTentativi() < 12 && !info.getResultGame()) {
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
    public void SetQuitUtente(String username) {
        InfoSessioneUtente tmpInfo = Tentativi.get(username);
        if (tmpInfo != null) {tmpInfo.setQuitGame(true);}
    }
    public void setTentativi() {Tentativi = new ConcurrentHashMap<>();}
    public void putAllTentativi(ConcurrentHashMap<String, InfoSessioneUtente> all) {Tentativi = all;}
    public void putInfo(String username, InfoSessioneUtente info) {Tentativi.put(username, info);}
    public void setWord(String w) {word = w;}
    public String getWord() {return word;}
    public void setCurrentTime(long Time) {currentTime = Time;}
    public long getCurrentTime() {return currentTime;}
    public void setNextTime(long Time) {nextTime = Time;}
    public long getNextTime() {return nextTime;}
    public String getTranslatedWord() {return translatedWord;}
    public void setTranslatedWord(String word) {translatedWord = word;}
    public int gettentativiUtente(String username) {
        return Tentativi.get(username).getTentativi();
    }

    public ConcurrentHashMap<String, InfoSessioneUtente> getTentativi() {return Tentativi;}
    public void setTentativi(ConcurrentHashMap<String, InfoSessioneUtente> tentativi) {Tentativi = tentativi;}
}