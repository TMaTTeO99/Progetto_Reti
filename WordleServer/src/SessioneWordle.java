import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

//Classe che implementa serializable in modo tale da poter salvare la sessione corrente di gioco
//e recuperarla quando il server viene chiuso

public class SessioneWordle implements Serializable {

    private String word; //parola di una sessione di gioco
    private String translatedWord; //traduzione della parola
    private long currentTime;//variabile per tenere traccia del tempo della parola corrente del gioco
    private long nextTime;//variabile che indica dopo quanto tempo verra creata la nuova parola

    private ConcurrentHashMap<String, InfoSessioneUtente> Tentativi;

    @JsonCreator
    public SessioneWordle() {Tentativi = new ConcurrentHashMap<>();}

    //metodo usato per l operazione di PlayWordle che il client richiede
    public int setGame(String username) {
        /*
        InfoSessioneUtente tmpInfo = Tentativi.get(username);//recupero l e info dell utente per questa sessione
        if(tmpInfo == null) {//se non ci sono info significa che il client non ha partecipato al gioco per questa sessione
            Tentativi.put(username, new InfoSessioneUtente(0, false));
        }
        else {
            if(tmpInfo.getTentativi() < 12 && !tmpInfo.getResultGame() && !tmpInfo.isQuitGame()) {return 1;}//1 indica che l utente ha gia inviato la richiesta di playWORDLE
            else {return -1;}//-1 indica che l utente ha gia partecipato al gioco
        }
        return 0;//0 indica richiesta di playWORDLE andata a buon fine

         */

        InfoSessioneUtente tmpInfo = null;
        //recupero l e info dell utente per questa sessione
        if((tmpInfo = Tentativi.putIfAbsent(username, new InfoSessioneUtente(0, false))) != null) {//se non ci sono info significa che il client non ha partecipato al gioco per questa sessione
            if(tmpInfo.getTentativi() < 12 && !tmpInfo.getResultGame() && !tmpInfo.isQuitGame()) {return 1;}//1 indica che l utente ha gia inviato la richiesta di playWORDLE
            else {return -1;}//-1 indica che l utente ha gia partecipato al gioco
        }
        return 0;//0 indica richiesta di playWORDLE andata a buon fine
    }
    public int Tentativo(String username) {//metodo usato per incrementare i tentativi effettuati

        InfoSessioneUtente info = Tentativi.get(username);//recupero le info dell utente per la sessione

        if(info == null)return -1;//caso in cui l utente non ha selezionato il comando playWORDLE
        if(info.isQuitGame())return -2;//caso in cui l utente ha gia giocato e ha effettuato il logout
        if(info.getTentativi() < 12 && !info.getResultGame()) {
            info.increaseTentativi();//aumento i tentativi effettuati se si hanno ancora tentativi disponibili
            return 0;
        }
        if(info.getTentativi() >= 12) return -2;//caso in cui l utente ha gia giocato e ha terminato i tentativi
        return -3; //ha vinto la partita precedentemente
    }
    public void setWinner(String UserName) {//metodo usato per settare che l utente ha vinto la partita
        InfoSessioneUtente tmp = Tentativi.get(UserName);
        tmp.setResultGame(true);
    }
    public boolean IsInGame(String username) {//metodo usato per controllare se l utente è in gioco
        InfoSessioneUtente u = Tentativi.get(username);
        if(u != null) return !u.getQuitGame() && u.getTentativi() >= 0 && u.getTentativi() < 12;
        return false;
    }
    public void SetQuitUtente(String username) {//metodo usato per settare che l utente ha abbandonato la partita
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
    public InfoSessioneUtente getInfoGameUtente(String username) {return Tentativi.get(username);}
    public void setTentativi(ConcurrentHashMap<String, InfoSessioneUtente> tentativi) {Tentativi = tentativi;}
}