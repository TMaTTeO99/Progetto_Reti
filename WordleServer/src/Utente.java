import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

//classe usata per rappresemtare un utente, ogni utente è identificato da un username che deve essere univoco, ad ogni untente poi
//sono associate diverse informazioni

public class Utente implements Serializable {

    private boolean Login = false; //variabile che indica se l'utente è loggato o meno
    private String Username = null;
    private String Passswd = null;
    private int Game = 0;//partite giocate
    private int WinGame = 0;//partite vinte
    private float WinGamePerc = 0;//percentuale partite vinte
    private int LastConsecutive = 0;//lunghezza ultima striscia positiva
    private int MaxConsecutive = 0;//striscia positiva piu lunga;
    transient private NotificaClient stub;//variabile per recuperare lo stub passato dal client nella fase di registrazione

    @JsonCreator //annotazione utilizzata per poter deserializzare i file
    public Utente(
            @JsonProperty("Username") String u,
            @JsonProperty("Passswd")String p) {
        Username = u;
        Passswd = p;
    }
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
        return Game;
    }

    public void setGame(int game) {
        Game = game;
    }

    public int getWinGame() {
        return WinGame;
    }

    public void setWinGame(int winGame) {
        WinGame = winGame;
    }

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

    public void setMaxConsecutive(int maxConsecutive) {
        MaxConsecutive = maxConsecutive;
    }

    public NotificaClient getStub() {return stub;}

    public void setStub(NotificaClient s) {stub = s;}

    public void RemoveSTub() { stub = null;}//metodo usato per eliminare lo stub prima di serializzare

    public boolean getLogin() {return Login;}

    public void setLogin() {Login = true;}

}
