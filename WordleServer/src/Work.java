import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Work implements Callable<PkjData> {

    private static final int SIZE_SIZE = 4;
    private SelectionKey Key;//variabile utilizzata per reperire e settare i dati per le comunicazioni come ad es il channell e l attach
    private Selector Selettore;
    private ConcurrentHashMap<String, Utente> Registrati;
    private int id;//intero che identifica la connessione
    private PkjData Dati;
    private ByteBuffer LenMexBuffer;
    private SessioneWordle Gioco;
    private Lock ReadWordLock;
    public Work(SelectionKey k, Selector s, ConcurrentHashMap<String, Utente> R, Integer ID, PkjData dati, ByteBuffer len, SessioneWordle g, Lock RWLock) {
        Key = k;
        Selettore = s;
        Registrati = R;
        id = ID;
        Dati = dati;
        LenMexBuffer = len;
        Gioco = g;
        ReadWordLock = RWLock;
    }
    public PkjData call() {

        //per implementare la soluzione non bloccante devo per prima cosa recuperare l attached e il channel

        SocketChannel channel = (SocketChannel) Key.channel();

        try {

            LenMexBuffer.flip();//resetto position per poter leggere dal ByteBuffer
            Dati.allocRequest(LenMexBuffer.getInt());//alloco il vettore che conterrà la richiesta dentro attached

            ByteBuffer RequestBuff = ByteBuffer.wrap(Dati.getRequest());
            while(channel.read(RequestBuff) != Dati.getRequest().length);
            //a questo punto quindi devo continuare a leggere da dovee avevo ripreso che è inidcato da dati.getIdxRequest()

            ParseRequest(Dati);
        }
        catch (Exception e) {e.printStackTrace();}

        return Dati;
    }

    //metodi privati per la gestione e il completamento delle richieste

    private void ParseRequest(PkjData dati) {

        //per ora è un metodo temporaneo che va scritto molto meglio, voglio solo testare un po di cose

        StringTokenizer Tok = new StringTokenizer(new String(dati.getRequest(), StandardCharsets.UTF_16), ":");
        String Method = Tok.nextToken();//recupero l'opeerazione che il client ha richiesto
        String username = null;
        String passwd = null;
        int lendati = 0;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;
        /**
         * Nota: qui posso usare delle classi per ogni risposta da dare per rendere il codice piu leggibile
         * Creare delle classi per ogni metodo e quindi usare i loro oggetti oppure metodi statici per
         * preparare la risposta, per ora lascio com'è solo per testare le idee
         */
        switch(Method) {
            case "login" :

                username = Tok.nextToken(" ").replace(":", "");//recupero username
                passwd = Tok.nextToken(" ");//recupero passwd

                dati.setUsname(username);//inserisco l'username nel pacchetto in modo che il thread che
                                         //gestisce le connessioni possa effettuare il logout in caso
                                         //il client chiuda la conn all improvviso
                //preparo la risposta per il client, la risposta ha lo stesso formato della richiesta
                lendati = "login:".length();//lunghezza dei dati
                dati.allocAnswer(lendati + 8 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio e l'intero finale che indica lo stato dell operazione

                SupportOut = new ByteArrayOutputStream();
                try (DataOutputStream OutWriter = new DataOutputStream(SupportOut)){

                    OutWriter.writeInt(lendati + 8);
                    OutWriter.writeChars("login:");

                    if((u = Registrati.get(username)) != null) {
                        if(u.getPassswd().equals(passwd)) {
                            if(u.getLogin()) {OutWriter.writeInt(-3);}//utente gia loggato
                            else {//utente non ancora loggato
                                u.setLogin(true);
                                u.setID_CHANNEL((Integer) Key.attachment());
                                OutWriter.writeInt(0);
                            }
                        }
                        else OutWriter.writeInt(-2);//-2 indica che l utente non ha inserito correttamente la passwd
                    }
                    else OutWriter.writeInt(-1);// -1 indica utente non registrato

                    dati.SetAnswer(SupportOut.toByteArray());
                }
                catch (Exception e) {e.printStackTrace();}
                break;
            case "logout":

                username = Tok.nextToken(" ").replace(":", "");//recupero username
                dati.setUsname(username);//analogo al ramo di login
                //preparo la risposta per il client, la risposta ha lo stesso formato della richiesta
                lendati = "logout:".length();//lunghezza dei dati
                dati.allocAnswer(lendati + 8 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio e l'intero finale che indica lo stato dell operazione

                //---------------------------------------------------------------
                SupportOut = new ByteArrayOutputStream();
                try (DataOutputStream OutWriter = new DataOutputStream(SupportOut)){

                    OutWriter.writeInt(lendati + 8);
                    OutWriter.writeChars("logout:");
                    if((u = Registrati.get(username)) != null) {

                        if(u.getLogin()) {

                            if(u.getID_CHANNEL() == (Integer) Key.attachment()) {
                                u.setLogin(false);
                                OutWriter.writeInt(0);
                            }
                            else OutWriter.writeInt(-3);//-2 indica che l utente non ha inserito l'username corretto
                        }
                        else OutWriter.writeInt(-2);//-2 indica che l utente non è loggato
                    }
                    else OutWriter.writeInt(-1);// -1 indica utente non registrato

                    dati.SetAnswer(SupportOut.toByteArray());
                }
                catch (Exception e) {e.printStackTrace();}
                //-----------------------------------------------------
                break;
            case "playWORDLE"://caso in cui un utente richiede di iniziare una partita
                username = Tok.nextToken(" ").replace(":", "");//recupero username
                dati.setUsname(username);//analogo al ramo di login

                lendati = "playWORDLE:".length();//lunghezza dei dati
                dati.allocAnswer(lendati + 8 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio e l'intero finale che indica lo stato dell operazione

                //---------------------------------------------------------------
                SupportOut = new ByteArrayOutputStream();
                try (DataOutputStream OutWriter = new DataOutputStream(SupportOut)){

                    OutWriter.writeInt(lendati + 8);
                    OutWriter.writeChars("playWORDLE:");
                    ReadWordLock.lock();
                    if(Gioco.setTentativi(username)) {
                        OutWriter.writeInt(0);//0 indica che l utente puo cominciare a giocare
                    }
                    else OutWriter.writeInt(-1);// -1 indica che l utente ha terminato i tentativi per questa sessione di gioco
                    dati.SetAnswer(SupportOut.toByteArray());
                }
                catch (Exception e) {e.printStackTrace();}
                finally {ReadWordLock.unlock();}
                break;
        }
    }
}
