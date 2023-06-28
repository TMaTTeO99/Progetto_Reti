import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLSyntaxErrorException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//classe usata per costruire il file json per la serializzazione
public class MakeJson implements Runnable{

    private ConcurrentHashMap<String, Utente> Registrati;//utenti registrati
    private LinkedBlockingDeque<DataToSerialize<?>> UDSlist;//lista che conterra gli user name degli utenti da serializzare
    private String PathJSN;//Stringa che contiene il path di dove scrivere i file json
    private String FileNameJsonUtenti = "DataStorageUtenti.json";//stringa che verra usata per creare il file json degli utenti
    private String FileNameJsonGame = "DataStorageGame.json";//stringa che verra usata per creare il file json del gioco
    private String FielNameJsonClassifica = "DataStorageClassifica.json";//stringa che verra usata per creare il file json della classifica
    private Lock ReadLockGame;//lock usata per poter leggere l istanza del gioco in mutua esclusione
                              //potrebbero succedere che il thread che crea una nuova sessione stia
                              //creando una nuova sessione mentre sto serializzando
    private Lock ReadLockClassifica;//analogo a readlockgame ma per la classifica
    private SessioneWordle Game;//oggetto che rappresenta l istanza del gioco
    private ArrayList<UserValoreClassifica> Classifica;//oggetto classifica
    private int AfterUpDate;//variabile usata per aggiornare il file json che continene gli utenti
                            //dopo che il contatore arriva a tale numero in modo da non serializzare sempre perche è una operazione lenta
    public MakeJson(ConcurrentHashMap<String, Utente> Utenti, LinkedBlockingDeque<DataToSerialize<?>> UDSL,
                    String PathJson, Lock RDlock, SessioneWordle g, ArrayList<UserValoreClassifica> Clss, Lock RDClass, int Aupdate) {

        AfterUpDate = Aupdate;
        Registrati = Utenti;
        UDSlist = UDSL;
        PathJSN = PathJson;
        ReadLockGame = RDlock;
        ReadLockClassifica = RDClass;
        Game = g;
        Classifica = Clss;
    }
    private FileWriter CheckAndDeserializeUntenti(String name, ObjectMapper map) {

        File tmp = null;
        FileWriter writefile = null;//file writer per poter settare la modalità di scrittura append

        try {
            tmp = new File(name);//controllo l'esistenza del file

            //se il file non esiste sono al primo avvio server quindi ritorno il file
            if(!tmp.exists()){return new FileWriter(name, true);}

            //Altrimenti se il file esiste devo deserializzare
            writefile = new FileWriter(name, true);//file usato per il generator in modalità lettura

            JsonFactory factory = new JsonFactory();
            JsonParser pars = factory.createParser(tmp);
            pars.setCodec(map);

            while(pars.nextToken() == JsonToken.START_OBJECT) {

                Utente u = pars.readValueAs(Utente.class);
                u.setReadWriteLock();
                u.setLogin(false);
                Registrati.put(u.getUsername(), u);
            }
        }
        catch (Exception e) { e.printStackTrace();}
        System.out.println("TERMINATA DESERIALIZZAZIONE UTENTI");
        return writefile;
    }
    private int DeserializeGame(String name, ObjectMapper map) {

        File GameJson = new File(name);
        if(!GameJson.exists())return 1;//ritorno se il file non esiste quindi sono all inizio

        try {

            JsonFactory factory = new JsonFactory();
            JsonParser pars = factory.createParser(GameJson);
            pars.setCodec(map);

            while(pars.nextToken() != null) {//scorro tutto il file json

                String field = pars.currentName();//recupero il field che sto cercando
                if(field != null) {
                    switch (field) {
                        case "word" :
                            pars.nextToken();//mi sposto sul valore del field
                            Game.setWord(pars.getText());
                            break;
                        case "translatedWord" :
                            pars.nextToken();//mi sposto sul valore del field
                            Game.setTranslatedWord(pars.getText());
                            break;
                        case "currentTime" :
                            pars.nextToken();//mi sposto sul valore del field
                            Game.setCurrentTime(pars.getLongValue());
                            break;
                        case "nextTime" :
                            pars.nextToken();//mi sposto sul valore del field
                            Game.setNextTime(pars.getLongValue());
                            break;
                        case "tentativi":
                            pars.nextToken();//mi sposto all inizio dell oggetto
                            while(pars.nextToken() != JsonToken.END_OBJECT) {
                                String usname = pars.getCurrentName();//recupero il nome dell utente
                                pars.nextToken();//mi sposto sul valore che sara l oggetto infosessioneutente
                                InfoSessioneUtente tmpinfo = pars.readValueAs(InfoSessioneUtente.class);
                                Game.putInfo(usname, tmpinfo);//inserisco i dati
                            }
                            break;
                    }
                }

            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        System.out.println("TERMINATA DESERIALIZZAZIONE GAME");
        return 0;
    }
    public int DeserializeClassifica(String name, ObjectMapper map) {

        File FileClassifica = new File(name);
        if(!FileClassifica.exists())return 1;//se il file non esiste sono all inizio e non devo serializzare

        try {

            JsonFactory factory = new JsonFactory();
            JsonParser pars = factory.createParser(FileClassifica);
            pars.setCodec(map);

            while(pars.nextToken() == JsonToken.START_OBJECT) {
                Classifica.add(pars.readValueAs(UserValoreClassifica.class));
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        System.out.println("TERMINATA DESERIALIZZAZIONE CLASSIFICA");
        return 0;
    }
    public void run() {

        int NumUpdate = 0;//variabile che mi tiene traccia del numero di utenti che hanno aggiornato le loro statistiche

        NotificaClient stub = null;
        FileWriter NewJsonUtenti = null;
        FileWriter NewJsonSessione = null;
        FileWriter NewJsonClassifica = null;
        FileWriter JsonFileUtenti = null;

        File TestDire = new File(PathJSN);

        //caso in cui la dir non esiste => la creo
        try {
            if(!TestDire.exists()) {TestDire.mkdir();}
        }
        catch (Exception e){
            System.out.println("Impossibile creare cartella di serializzazione");
            System.out.println("I dati non verranno serializzati");
            System.out.println("Controllare impostazioni security menager");
            return;
        }

        JsonGenerator generator = null;
        ObjectMapper map = new ObjectMapper();
        map.enable(SerializationFeature.INDENT_OUTPUT);
        map.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        try {
            //ora devo controllare se il file esiste e in tal caso scorrerlo e deserializzare
            if((JsonFileUtenti = CheckAndDeserializeUntenti(PathJSN.concat("/").concat(FileNameJsonUtenti), map)) == null) {throw new NullPointerException();}
            if(DeserializeGame(PathJSN.concat("/").concat(FileNameJsonGame), map) == -1) throw new NullPointerException();
            if(DeserializeClassifica(PathJSN.concat("/").concat(FielNameJsonClassifica), map) == -1)throw new NullPointerException();


            JsonFactory factory = new JsonFactory();
            generator = factory.createGenerator(JsonFileUtenti);
            generator.setCodec(map);
            generator.useDefaultPrettyPrinter();

            while(!Thread.interrupted()) {


                DataToSerialize dato = UDSlist.takeFirst();
                char flag = dato.getFlag();//recupero il campo flag che indica che tipo di dato sto ricevendo

                switch (flag) {

                    case 'N' : //caso in cui il lista sarà presente l username di un utente
                              //in questo caso quindi quando ricevo 'N' indica username di
                              //utente che deve essere serializzato dall inizio, quando è appena iscritto
                        Utente u = (Utente) dato.getDato();
                        if(u != null) {
                            try {
                                u.getReadLock().lock();
                                generator.writeObject(u);
                            }
                            finally {u.getReadLock().unlock();}
                        }
                        break;
                    case 'U' ://Coso in cui un utente ha aggiornato i dati statistici
                              //Uso un intero per controllare quanti utenti hanno aggiornato i loro dati, serializzo quando un certo
                              //numero di utenti hanno aggiornato le statistiche in modo da non dover accedere al disco troppo
                              //frequentemente visto che bisognerà riscrivere tutto il file json

                        NumUpdate++;//aggiorno il numero di utenti che hanno aggiornato le loro statistiche
                        if(NumUpdate >= AfterUpDate) {
                            MakeJsonUPdateUtenti(generator, map, factory, NewJsonUtenti);
                            NumUpdate = 0;
                        }
                        break;
                    case 'C' : // 'C' indica che bisogna serializzare la classifica
                        MakeJsonUpdateClassifica(map, factory, NewJsonClassifica);
                        break;
                    case 'I' : // 'I' indica che bisogna serializzare l istanza attuale del gioco

                        MakeJsonUpdateGame(map, factory, NewJsonSessione, dato);
                        break;
                }
                //condizione di controllo della stringa per la terminazione del thread
                if(flag == 'S') {//condizione di terminazione del thread che serializza S sta per stop
                    System.out.println("Il server sta chiudendo quindi esco");//stampa di prova
                    break;
                }
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

    //metodi privati per serializzare i dati
    private void MakeJsonUpdateGame(ObjectMapper map, JsonFactory factory, FileWriter NewJsonSessione, DataToSerialize dato) throws Exception{

        NewJsonSessione = new FileWriter(PathJSN.concat("/").concat("tempSessione.json"));
        JsonGenerator genSessione = factory.createGenerator(NewJsonSessione);
        genSessione.setCodec(map);

        try {
            ReadLockGame.lock();
            genSessione.writeObject(dato.getDato());//serializzo l istanza del game
        }
        finally {ReadLockGame.unlock();}

        //rinomino i file e distruggo quelli vecchi
        File oldJsonGame = new File(PathJSN.concat("/").concat(FileNameJsonGame));
        File RenameFileGame = new File(PathJSN.concat("/").concat("tempSessione.json"));

        oldJsonGame.delete();
        RenameFileGame.renameTo(oldJsonGame);

    }
    private void MakeJsonUPdateUtenti(JsonGenerator gen, ObjectMapper map, JsonFactory factory, FileWriter NewJsonUtenti) throws Exception{

        //a questo punto devo saerializzare i dati:: Attenzione il NewJsonUtenti dovrebbe essere aperto in mod append

        NewJsonUtenti = new FileWriter(PathJSN.concat("/").concat("tempUtenti.json"));
        gen = factory.createGenerator(NewJsonUtenti);
        gen.setCodec(map);
        Collection<Utente> lst =  Registrati.values();

        for(Utente Giocataore : lst) {
            try {
                Giocataore.getReadLock().lock();
                gen.writeObject(Giocataore);
            }
            finally {Giocataore.getReadLock().unlock();}
        }

        File oldJson = new File(PathJSN.concat("/").concat(FileNameJsonUtenti));
        File RenameFile = new File(PathJSN.concat("/").concat("tempUtenti.json"));

        oldJson.delete();
        RenameFile.renameTo(oldJson);

    }
    private void MakeJsonUpdateClassifica(ObjectMapper map, JsonFactory factory, FileWriter NewJsonClassifica) throws Exception{

        NewJsonClassifica = new FileWriter(PathJSN.concat("/").concat("tempClassifica.json"));
        JsonGenerator genClassifica = factory.createGenerator(NewJsonClassifica);
        genClassifica.setCodec(map);

        try {
            ReadLockClassifica.lock();
            for(int i = 0; i<Classifica.size(); i++) {
                genClassifica.writeObject(Classifica.get(i));
            }
        }
        finally {ReadLockClassifica.unlock();}

        File oldJsonClass = new File(PathJSN.concat("/").concat(FielNameJsonClassifica));
        File RenameFileClass = new File(PathJSN.concat("/").concat("tempClassifica.json"));

        oldJsonClass.delete();
        RenameFileClass.renameTo(oldJsonClass);

    }

}
