import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class InfoSessioneUtente implements Serializable {

    private int tentativi;//tentativi effettuati dagli utenti per indovinare la parola
    private boolean resultGame;//flag per indicare se l utente ha indovinato la partita, true indovinata, false altrimenti

    @JsonCreator
    public InfoSessioneUtente(@JsonProperty("tentativi") int t, @JsonProperty("resultGame")boolean rs) {
        tentativi = t;
        resultGame = rs;
    }

    public void setResultGame(boolean rs) {resultGame = rs;}
    public void setTentativi(int n) {tentativi = n;}
    public void increaseTentativi() {tentativi++;}
    public int getTentativi() {return tentativi;}
    public boolean getResult() {return resultGame;}
}
