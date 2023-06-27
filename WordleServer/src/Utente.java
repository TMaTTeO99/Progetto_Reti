import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//classe usata per rappresemtare un utente, ogni utente è identificato da un username che deve essere univoco, ad ogni untente poi
//sono associate diverse informazioni

public class Utente implements Serializable {



    private HashMap<UUID, InfoLogin> LoginChannel = new HashMap<>();//HashMap usata per mantenere le info riguardo al login
    private String Username = null;
    private String Passswd = null;
    private int Games = 0;//partite giocate
    private int WinGame = 0;//partite vinte
    private float WinGamePerc = 0;//percentuale partite vinte
    private int LastConsecutive = 0;//lunghezza ultima striscia positiva
    private int MaxConsecutive = 0;//striscia positiva piu lunga;
    private int [] GuesDistribuition = new int[12]; //Il numero massimo di tentativi che un utente puo fare è 12
    private NotificaClient stub;//variabile per recuperare lo stub passato dal client nella fase di registrazione

    //lock usate per accedere in mutua esclusione ai dati dell utente
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock ReadLock = lock.readLock();
    private Lock WriteLock = lock.writeLock();

    @JsonCreator //annotazioni utilizzate per poter deserializzare i file
    public Utente(
            @JsonProperty("Username") String u,
            @JsonProperty("Passswd") String p) {
        Username = u;
        Passswd = p;
    }
    @JsonIgnore //annotazione per non far serializzare le variabili di istanza che riguardano le var di lock
    public ReentrantReadWriteLock getLock() {return lock;}
    @JsonIgnore //annotazione per non far serializzare le variabili di istanza che riguardano le var di lock
    public Lock getReadLock() {return ReadLock;}
    @JsonIgnore //annotazione per non far serializzare le variabili di istanza che riguardano le var di lock
    public Lock getWriteLock() {return WriteLock;}
    @JsonIgnore //annotazione per non far serializzare lo stub
    public NotificaClient getStub() {return stub;}
    public void setGuesDistribuition(int [] g) {GuesDistribuition = g;}//usato per deserializzare
    public int [] getGuesDistribuition() {return GuesDistribuition;}
    public String getPassswd() {return Passswd;}

    public void setPassswd(String passswd) {Passswd = passswd;}//usato per deserializzare

    public String getUsername() {return Username;}

    public void setUsername(String username) {Username = username;}
    public int getGame() {return Games;}
    public void setGame(int game) {Games = game;}//usato per deserializzare
    public void increasesGame() {Games += 1;}
    public int getWinGame() {return WinGame;}
    public void setWinGame(int winGame) {WinGame = winGame;}//usato per deserializzare
    public void increasesWinGame() {WinGame += 1;}
    public float getWinGamePerc() {return WinGamePerc;}
    public void setWinGamePerc(float winGamePerc) {WinGamePerc = winGamePerc;}//usato per deserializzare
    public int getLastConsecutive() {return LastConsecutive;}
    public void setLastConsecutive(int lastConsecutive) {LastConsecutive = lastConsecutive;}//usato per deserializzare
    public int getMaxConsecutive() {return MaxConsecutive;}
    public int getGuesDistribuition(int idx) {return GuesDistribuition[idx];}
    public void setGuesDistribuition(int idx, int guesDistribuition) {GuesDistribuition[idx] = guesDistribuition;}
    public void setMaxConsecutive(int maxConsecutive) {MaxConsecutive = maxConsecutive;}//usato per deserializzare
    public HashMap<UUID, InfoLogin> getLoginChannel() {return LoginChannel;}
    public void setLoginChannel(HashMap<UUID, InfoLogin> info) {LoginChannel = info;}//usato per deserializzare
    public void UpdatePercWingame() {WinGamePerc = ( (float) (WinGame * 100) / (float)Games);}
    public void setStub(NotificaClient s) {stub = s;}
    public void RemoveSTub() {stub = null;}//metodo usato per eliminare lo stub prima di serializzare
    public void setLogin(UUID idx, boolean val) { LoginChannel.put(idx, new InfoLogin(Username, val));}//val == 1 login, val == 0 logout
    public void updateLastConsecutive(boolean flag) {

        if(flag) {//flag == true => il metodo viene chiamato quando il client ha vinto la partita
            LastConsecutive += 1;
            if(LastConsecutive > MaxConsecutive) MaxConsecutive = LastConsecutive;
        }
        else {LastConsecutive = 0;}//caso  in cui il client ha finito i tentativi senza indovinare la parola allora si interrompe la striscia positiva
    }
    public void setReadWriteLock() {
        lock = new ReentrantReadWriteLock();
        ReadLock = lock.readLock();
        WriteLock = lock.writeLock();
    }

    public String getUserLogin(UUID idx) {

        InfoLogin info = LoginChannel.get(idx);
        if(info != null)return info.getName();

        return null;
    }
    public boolean getLogin(UUID idx) {

        InfoLogin info = LoginChannel.get(idx);
        if(info != null) return info.getlogin();
        return false;
    }
}
