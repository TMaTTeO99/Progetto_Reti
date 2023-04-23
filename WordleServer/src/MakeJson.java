import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

//classe usata per costruire il file json per la serializzazione
public class MakeJson implements Runnable{

    private HashMap<String, Utente> Registrati;//utenti registrati
    private LinkedBlockingDeque<String> UDSlist;//lista che conterra gli user name degli utenti da serializzare

    public MakeJson(HashMap<String, Utente> Utenti, LinkedBlockingDeque<String> UDSL) {
        Registrati = Utenti;
        UDSlist = UDSL;
    }

    public void run() {
        /**
         * WORNINGGG::::::
         * Per ora uso un file che creo qua come prova, in realta tale file dovrà essere recuperato
         * dal file di configurazione, inoltre per ora sto facendo solo dei test per settare al meglio la creazione
         * del json, poi devo modificare
         */
        File testFile = new File("../fileprova.json");
        JsonGenerator generator = null;
        try {
            JsonFactory factory = new JsonFactory();
            generator = factory.createGenerator(testFile, JsonEncoding.UTF8);
            generator.setCodec(new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS));
            generator.writeStartArray();
            generator.useDefaultPrettyPrinter();
            while(!Thread.interrupted()) {
                //qui andra inserita una condizione di controllo della stringa
                //perche quando dovro far terminare questo thread per svegliarlo
                //inseriro nella coda una stringa speciale che quindi non userà per
                // serializzera e terminera
                String username = UDSlist.takeFirst();
                Utente u = Registrati.get(username);
                generator.writeObject(u);
                break;
            }
            generator.writeEndArray();
            System.out.println("Interruzione Servizio di salvataggio dati");
        }
        catch (Exception e) {
            e.printStackTrace();
            if(e instanceof InterruptedException) {
                System.out.println("Interruzione Servizio di salvataggio dati");
                try {
                    generator.writeEndArray();
                }
                catch (IOException ee){
                    ee.printStackTrace();
                    System.out.println("Errore nella scrittura del file json");
                }
            }
            else if(e instanceof IOException) {System.out.println("Errore nella scrittura del file json");}
        }
    }
}
