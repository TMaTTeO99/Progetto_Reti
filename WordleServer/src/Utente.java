import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

//classe usata per rappresemtare un utente, ogni utente è identificato da un username che deve essere univoco, ad ogni untente poi
//sono associate diverse informazioni

public class Utente implements Serializable {

    private int ID_CHANNEL = -1;
    private boolean login = false; //variabile che indica se l'utente è loggato o meno, Nota: a jackson da noia se scrivo Login
    private String Username = null;
    private String Passswd = null;
    private int Games = 0;//partite giocate
    private int WinGame = 0;//partite vinte
    private float WinGamePerc = 0;//percentuale partite vinte
    private int LastConsecutive = 0;//lunghezza ultima striscia positiva
    private int MaxConsecutive = 0;//striscia positiva piu lunga;
    private float [] GuesDistribuition = new float[12]; //Il numero massimo di tentativi che un utente puo fare è 12
    private NotificaClient stub;//variabile per recuperare lo stub passato dal client nella fase di registrazione

    @JsonCreator //annotazioni utilizzate per poter deserializzare i file
    public Utente(
            @JsonProperty("Username") String u,
            @JsonProperty("Passswd")String p) {
        Username = u;
        Passswd = p;
    }
    public void setGuesDistribuition(float [] g) {GuesDistribuition = g;}

    public float [] getGuesDistribuition() {return GuesDistribuition;}
    public String getPassswd() {
        return Passswd;
    }

    public void setPassswd(String passswd) {
        Passswd = passswd;
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public int getGame() {
        return Games;
    }

    public void setGame(int game) {
        Games = game;
    }

    public void increasesGame() {Games += 1;}

    public int getWinGame() {
        return WinGame;
    }

    public void setWinGame(int winGame) {
        WinGame = winGame;
    }

    public void increasesWinGame() {WinGame += 1;}

    public float getWinGamePerc() {
        return WinGamePerc;
    }

    public void setWinGamePerc(float winGamePerc) {
        WinGamePerc = winGamePerc;
    }

    public int getLastConsecutive() {
        return LastConsecutive;
    }

    public void setLastConsecutive(int lastConsecutive) {
        LastConsecutive = lastConsecutive;
    }


    public int getMaxConsecutive() {
        return MaxConsecutive;
    }

    public boolean getLogin() {return login;}

    public void setLogin(boolean value) {login = value;}

    public float getGuesDistribuition(int idx) {return GuesDistribuition[idx];}

    public void setGuesDistribuition(int idx, float guesDistribuition) {GuesDistribuition[idx] = guesDistribuition;}

    public void setMaxConsecutive(int maxConsecutive) {
        MaxConsecutive = maxConsecutive;
    }

    public NotificaClient getStub() {return stub;}

    public void setStub(NotificaClient s) {stub = s;}

    public void RemoveSTub() { stub = null;}//metodo usato per eliminare lo stub prima di serializzare

    public int getID_CHANNEL() {
        return ID_CHANNEL;
    }

    public void setID_CHANNEL(int ID) {
        ID_CHANNEL = ID;
    }

    public void UpdatePercWingame() {WinGamePerc = ( ((float) Games / (float) 100) * WinGame);}

    public void updateLastConsecutive(boolean flag) {

        if(flag) {//flag == true => il metodo viene chiamato quando il client ha vinto la partita
            LastConsecutive += 1;
            if(LastConsecutive > MaxConsecutive) MaxConsecutive = LastConsecutive;
        }
        else {LastConsecutive = 0;}//caso  in cui il client ha finito i tentativi senza indovinare la parola allora si interrompe la striscia positiva
    }

}
