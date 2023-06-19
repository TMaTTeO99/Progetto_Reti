import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;

//classe che conterra un metodo run per poter periodicamente creare un nuova parola
public class OpenGame implements Runnable{

    private long time;//tempo che intercorre fra la publicazione di una parola e la successiva
    private SessioneWordle game;//oggetto che rappresenta una sessione di gioco
    private long lasttime;//tempo in millisecondi della precedente creazione di un gioco, tale info
                          //viene estrapolata dal file di config
    private long currenttime;//variabile di istanza per salvare il tempo corrente
    private ArrayList<String> Vocabolario;//parola del gioco
    private File ConfigureFile;//file di configurazione che deve essere aggiornato alla chiusura del server per inserire il timestamp
                               //Dell ultima volta in cui è stata estratta una parola
    private SessioneWordle Game;//variabile che rappresenta il gioco
    private Lock lock;//lock per implementare mutua esclusione fra per i thread che accedono all istanza del gioco
    private String URLtransale;//URL del servizio di traduzione
    private LinkedBlockingDeque<DataToSerialize> DaSerializzare;
    public OpenGame(long t, long lt, ArrayList<String> Vcb, File ConfFile, SessioneWordle gm, Lock lck, String URL, LinkedBlockingDeque<DataToSerialize> SerializeQueue) {
        lasttime = lt;
        time = t;
        Vocabolario = Vcb;
        ConfigureFile = ConfFile;
        Game = gm;
        lock = lck;
        URLtransale = URL;
        DaSerializzare = SerializeQueue;
    }
    public void run() {

        Random randword = new Random();
        while(!Thread.interrupted()){
            //qui devo controllare che sia passato il giusto intervallo di tempo fra la precedente
            //creazione di un gioco e il momento corrente
            try {
                currenttime = System.currentTimeMillis();
                if((currenttime - lasttime) < time) {
                    Thread.sleep(time - (currenttime - lasttime));
                }
                String tmp = Vocabolario.get(randword.nextInt(Vocabolario.size()));
                System.out.println("Parola del gioco " + tmp);
                lock.lock();
                     Game.setWord(tmp);
                     Game.setTranslatedWord(TranslateService(tmp));
                     Game.setNextTime(time);
                     lasttime = System.currentTimeMillis();
                     currenttime = System.currentTimeMillis();
                     Game.setCurrentTime(currenttime);
                     Game.setTentativi();
                     try {DaSerializzare.put(new DataToSerialize<>(Game, 'I'));}
                     catch (Exception e) {e.printStackTrace();}
                lock.unlock();
                WriteLastSpawn(lasttime);//modifico il file di config in modo da scriverci dentro il time stamp dell ultima
                                        //sessione di gioco creata

                System.out.println("Game creato");
            }
            catch (Exception e) {
                e.printStackTrace();
                lock.unlock();
            }
        }
    }
    private String TranslateService(String GameWord) {//metodo privato usato per recuperare i la traduzione della parola

        String wordTradotta = new String();
        try {

            URL Service = new URL(URLtransale + "get?q=" + GameWord + "&langpair=en|it");
            URLConnection Request = Service.openConnection();

            try(BufferedInputStream in = new BufferedInputStream(new DataInputStream(Request.getInputStream()))) {

                //dopo aver recuperato la risposta devo recuperare la traduzione della parola
                //deserializzando la risposta
                JsonFactory factory = new JsonFactory();
                JsonParser pars = factory.createParser(in);

                while(pars.nextToken() != null) {//scorro tutti i dati
                    String currentField = pars.currentName();//recupero il field corrente

                    if(currentField != null) {
                        if(pars.currentName().equals("translation")) {//se è quello cercato

                            pars.nextToken();//considero il valore di translation
                            wordTradotta = pars.getText();//recupero la parola tradotta
                            System.out.println(wordTradotta + "  <--- Traduzione");
                            break; //recupero la traaduzione ed esco
                        }
                    }
                }
            }
            catch (Exception e) {
                wordTradotta = wordTradotta.concat("TRADUZIONE NON DISPONIBILE");
            }

        }
        catch (Exception e) {
            wordTradotta = wordTradotta.concat("TRADUZIONE NON DISPONIBILE");
        }
        return wordTradotta;
    }

    private void WriteLastSpawn(long lasttime) {

        FileWriter NewConfg = null;
        try (BufferedReader in = new BufferedReader(new FileReader(ConfigureFile));
            BufferedWriter ou = new BufferedWriter(NewConfg = new FileWriter(ConfigureFile.getParent().concat("/tmpConfig.txt")))){

            String line = null;
            char [] tmpline = new char[8];//8 sono il numero di caratteri di "lastWord" nel file di config
            while((line = in.readLine()) != null) {

                System.arraycopy(line.toCharArray(), 0, tmpline, 0, 8);

                if(String.valueOf(tmpline).equals("lastWord")) {//se ho letto il campo lastWord nel file di config
                    ou.write("lastWord="+Long.toString(lasttime)+"\n");
                }
                else {//altrimenti copio le altre info nel file
                    ou.write(line+"\n");
                }
            }
            //a questo punto elimino il file vecchio e rinomino quello nuovo
            File oldFile = new File(ConfigureFile.getPath());
            File renameFile = new File(ConfigureFile.getParent().concat("/tmpConfig.txt"));

            oldFile.delete();
            renameFile.renameTo(oldFile);

        }
        catch (Exception e) {e.printStackTrace();}


    }
}
