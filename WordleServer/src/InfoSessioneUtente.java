import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class InfoSessioneUtente implements Serializable {

    private int tentativi;//tentativi effettuati dagli utenti per indovinare la parola
    private boolean resultGame;//flag per indicare se l utente ha indovinato la partita, true indovinata, false altrimenti

    private boolean QuitGame;//variabile che indica che l utente ha provato ha giocare la partita ma ha effettuato il logout
                             //ancor prima di terminare i tentativi, viene settata a true quando succede.
    @JsonCreator
    public InfoSessioneUtente(@JsonProperty("tentativi") int t, @JsonProperty("resultGame")boolean rs) {
        tentativi = t;
        resultGame = rs;
        QuitGame = false;
    }

    public void setResultGame(boolean rs) {resultGame = rs;}
    public void setTentativi(int n) {tentativi = n;}
    public void increaseTentativi() {tentativi++;}
    public int getTentativi() {return tentativi;}
    public boolean getResult() {return resultGame;}
    public boolean isQuitGame() {return QuitGame;}
    public void setQuitGame(boolean quitGame) {QuitGame = quitGame;}
}
