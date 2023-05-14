import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Work implements Callable<PkjData> {

    private static final int SIZE_SIZE = 4;
    private SelectionKey Key;//variabile utilizzata per reperire e settare i dati per le comunicazioni come ad es il channell e l attach
    private Selector Selettore;
    private Lock write;//lock usata per accedere alla lista degli utenti registarti e cambiare i campi di un utente in base all operazione
    private HashMap<String, Utente> Registrati;
    private int id;//intero che identifica la connessione
    private PkjData Dati;
    private ByteBuffer LenMexBuffer;
    public Work(SelectionKey k, ReentrantReadWriteLock lock, Selector s, HashMap<String, Utente> R, Integer ID, PkjData dati, ByteBuffer len) {
        Key = k;
        Selettore = s;
        Registrati = R;
        write = lock.writeLock();
        id = ID;
        Dati = dati;
        LenMexBuffer = len;
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

        /**
         * Nota: qui posso usare delle classi per ogni risposta da dare per rendere il codice piu leggibile
         * Creare delle classi per ogni metodo e quindi usare i loro oggetti oppure metodi statici per
         * preparare la risposta, per ora lascio com'è solo per testare le idee
         */
        switch(Method) {
            case "login" :
                write.lock();
                String username = Tok.nextToken(" ").replace(":", "");//recupero username
                String passwd = Tok.nextToken(" ");//recupero passwd

                //preparo la risposta per il client, la risposta ha lo stesso formato della richiesta
                int lendati = "login:".length();//lunghezza dei dati
                dati.allocAnswer(lendati + 8 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio e l'intero finale che indica lo stato dell operazione

                ByteArrayOutputStream SupportOut = new ByteArrayOutputStream();
                try (DataOutputStream OutWriter = new DataOutputStream(SupportOut)){

                    OutWriter.writeInt(lendati + 8);
                    OutWriter.writeChars("login:");

                    if(Registrati.containsKey(username)){//controllo che l'utente inserito sia registratoì

                        if(Registrati.get(username).getPassswd().equals(passwd)) {
                            Registrati.get(username).setLogin(true);
                            OutWriter.writeInt(0);
                        }
                        else OutWriter.writeInt(-2);//-2 indica che l utente non ha inserito correttamente la passwd
                    }
                    else {
                        OutWriter.writeInt(-1);// -1 indica utente non registrato
                    }
                    dati.SetAnswer(SupportOut.toByteArray());
                }
                catch (Exception e) {e.printStackTrace();}
                finally {
                    write.unlock();
                }
                break;
            case "logout":
                break;
        }
    }
}
