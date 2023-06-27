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

        InfoSessioneUtente tmpInfo = null;

        //se non ci sono info significa che il client non ha partecipato al gioco per questa sessione alloras lo aggiungo
        if((tmpInfo = Tentativi.putIfAbsent(username, new InfoSessioneUtente(0, false))) != null) {

            //se sono presenti info dell utente per questa sessione faccio i vari controllli
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

    //metodo usato per controllare se l utente è in gioco oppure ha effettuato il logout prima di finire i tentativi
    public boolean IsInGame(String username) {
        InfoSessioneUtente u = Tentativi.get(username);
        if(u != null) return !u.getQuitGame() && u.getTentativi() >= 0 && u.getTentativi() < 12;
        return false;
    }
    public void SetQuitUtente(String username) {//metodo usato per settare che l utente ha abbandonato la partita (logout prima di aver terminato i tentativi)
        InfoSessioneUtente tmpInfo = Tentativi.get(username);
        if (tmpInfo != null) {tmpInfo.setQuitGame(true);}
    }
    public void setTentativi() {Tentativi = new ConcurrentHashMap<>();}//metodo usasato per resettare le info degli utenti associate alla sessione
    public void putAllTentativi(ConcurrentHashMap<String, InfoSessioneUtente> all) {Tentativi = all;}//usato solo per la deserializzazione
    public void putInfo(String username, InfoSessioneUtente info) {Tentativi.put(username, info);}//metodo usato per deserializzare
    public void setWord(String w) {word = w;}//metodo usato per settare la parola del gioco che viene estratta dal vocabolario
    public String getWord() {return word;}//metodo per recuperare la parola del game
    public void setCurrentTime(long Time) {currentTime = Time;}//metodo per settare il tempo di generazione della parola
    public long getCurrentTime() {return currentTime;}//metodo per recuperare il tempo di generazione della parola
    public void setNextTime(long Time) {nextTime = Time;}//metodo usato per salvare dopo quanto tempo viene generato il nuovo game usato per la richiesta di timenextword
    public long getNextTime() {return nextTime;}////metodo usato per recuperare dopo quanto tempo viene generato il nuovo game
    public String getTranslatedWord() {return translatedWord;}//metodo per recuperare la traduzione della parola
    public void setTranslatedWord(String word) {translatedWord = word;}//metodo usato per settare la traduzione della parola
    public int gettentativiUtente(String username) {return Tentativi.get(username).getTentativi();}//metodo per recuperare il numero di tentativi fatti
    public ConcurrentHashMap<String, InfoSessioneUtente> getTentativi() {return Tentativi;}//metodo usato poi poter inserire i suggerimenti per l utente
                                                                                            //che verranno usati quando l utente vuole condividerli
    public InfoSessioneUtente getInfoGameUtente(String username) {return Tentativi.get(username);}//metodo per recuperare le info della sessione del game dell utente
    public void setTentativi(ConcurrentHashMap<String, InfoSessioneUtente> tentativi) {Tentativi = tentativi;}//usato per deserializzare
}