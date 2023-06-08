import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class InfoLogin implements Serializable {

    private String Name;
    private boolean login;

    @JsonCreator
    public InfoLogin(@JsonProperty("Name") String user,
                     @JsonProperty("login") boolean lgin) {
        Name = user;
        login = lgin;
    }

    public String getName() {return Name;}

    public void setName(String nm) {Name = nm;}

    public boolean getlogin() {return login;}

    public void setlogin(boolean log) {login = log;}
}
