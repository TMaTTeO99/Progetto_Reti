import java.util.ArrayList;

//classe utilizzata per contenere i suggerimenti di un utente
public class Suggerimenti {
    private String Utente;
    private ArrayList<String> Suggerimenti = new ArrayList<>();//lista dei tentativi fatti dall utente
    public String getUtente() {return Utente;}
    public ArrayList<String> getSuggerimenti() {return Suggerimenti;}
    public void addSuggerimento(String suggerimento) {Suggerimenti.add(suggerimento);}
    public void setUtente(String user) {Utente = user;}
}
