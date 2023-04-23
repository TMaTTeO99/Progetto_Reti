import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Gioco implements Serializable {

    private String Word;//parola del giorno
    private Date DataStart;//data di inizio del gioco
    private Date DataEnd;//data di fine del gioco

    //formato standard delle date
    private SimpleDateFormat Format = new SimpleDateFormat("EEE dd MM yyyy HH:mm:ss, z", Locale.ITALIAN);
    public Gioco(String w, Date end) {
        Word = w;
        Format.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
        DataStart = new Date(System.currentTimeMillis());
        DataEnd = end;
    }

    public String getWord() {
        return Word;
    }

    public Date getDataStart() {
        return DataStart;
    }

    public Date getDataEnd() {
        return DataEnd;
    }
}
