import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class InfoLogin implements Serializable {

    private String Name;
    private boolean login;
    private NotificaClient stub;//variabile per recuperare lo stub passato dal client nella fase di registrazione

    @JsonCreator
    public InfoLogin(@JsonProperty("Name") String user,
                     @JsonProperty("login") boolean lgin) {
        Name = user;
        login = lgin;
    }
    @JsonIgnore //annotazione per non far serializzare lo stub
    public NotificaClient getStub() {return stub;}
    public void setStub(NotificaClient s) {stub = s;}
    public void RemoveSTub() {stub = null;}//metodo usato per eliminare lo stub prima di serializzare
    public String getName() {return Name;}
    public void setName(String nm) {Name = nm;}
    public boolean getlogin() {return login;}
    public void setlogin(boolean log) {login = log;}
}
