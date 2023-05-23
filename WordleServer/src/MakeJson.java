import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//classe usata per costruire il file json per la serializzazione
public class MakeJson implements Runnable{

    private ConcurrentHashMap<String, Utente> Registrati;//utenti registrati
    private LinkedBlockingDeque<String> UDSlist;//lista che conterra gli user name degli utenti da serializzare
    private String PathJSN;//Stringa che contiene il path di dove scrivere i file json
    private String FileNameJson = "DataStorage.json";//stringa che verra usata per creare i file json
    public MakeJson(ConcurrentHashMap<String, Utente> Utenti, LinkedBlockingDeque<String> UDSL, String PathJson) {

        Registrati = Utenti;
        UDSlist = UDSL;
        PathJSN = PathJson;
    }
    private FileWriter CheckAndDeserialize(String name, ObjectMapper map) {

        File tmp = null;
        FileWriter writefile = null;//file qwwriter per poter settare la modalità di scrittura append

        try {
            tmp = new File(name);//file per poter controllare l'esistenza del file
            writefile = new FileWriter(name, true);//file usato per il generator in modalità lettura
            if(!tmp.exists())return writefile;//se il file non esiste sono al primo avvio server quindi ritorno il file

            //Altrimenti se il file esiste devo deserializzare
            JsonFactory factory = new JsonFactory();
            JsonParser pars = factory.createParser(tmp);
            pars.setCodec(map);

            while(pars.nextToken() == JsonToken.START_OBJECT) {
                Utente u = pars.readValueAs(Utente.class);
                Registrati.put(u.getUsername(), u);
            }
        }
        catch (Exception e) { e.printStackTrace();}

        return writefile;
    }
    public void run() {

        FileWriter JsonFile = null;
        File TestDire = new File(PathJSN);
        if(!TestDire.exists()) { //caso in cui la dir non esiste => la creo
            TestDire.mkdir();
        }

        JsonGenerator generator = null;
        ObjectMapper map = new ObjectMapper();
        map.enable(SerializationFeature.INDENT_OUTPUT);
        map.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        try {
            //ora devo controllare se il file esiste e in tal caso scorrerlo e deserializzare
            if((JsonFile = CheckAndDeserialize(PathJSN.concat("/").concat(FileNameJson), map)) == null) {throw new NullPointerException();}

            JsonFactory factory = new JsonFactory();
            generator = factory.createGenerator(JsonFile);
            generator.setCodec(map);
            generator.useDefaultPrettyPrinter();

            while(!Thread.interrupted()) {


                String username = UDSlist.takeFirst();

                //condizione di controllo della stringa per la terminazione del thread
                if(username.equals("STOP_THREAD")) {//condizione di terminazione del thread che serializza
                    System.out.println("Il server sta chiudendo quindi esco");//stampa di prova
                    break;
                }
                Utente u = Registrati.get(username);
                u.RemoveSTub();
                generator.writeObject(u);
            }
            generator.close();
            System.out.println("Interruzione Servizio di salvataggio dati");
        }
        catch (Exception e) {
            e.printStackTrace();
            if(e instanceof InterruptedException) {System.out.println("Interruzione Servizio di salvataggio dati");}
            else if(e instanceof IOException) {System.out.println("Errore nella scrittura del file json");}
            else if(e instanceof NullPointerException) {System.out.println("Errore nella lettura del file json");}
        }
    }
}
