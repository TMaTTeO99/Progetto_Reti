import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    private ArrayList<String> Words;
    public Work(SelectionKey k, Selector s, ConcurrentHashMap<String, Utente> R, Integer ID, PkjData dati, ByteBuffer len, ArrayList<String> Vocabolario, SessioneWordle g, Lock RWLock) {
        Key = k;
        Selettore = s;
        Registrati = R;
        id = ID;
        Dati = dati;
        LenMexBuffer = len;
        Gioco = g;
        ReadWordLock = RWLock;
        Words = Vocabolario;
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
        String Method = Tok.nextToken();//recupero l'operazione che il client ha richiesto
        String username = null;
        String passwd = null;
        String word = null;
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
            case "logout" :

                username = Tok.nextToken(" ").replace(":", "");//recupero username
                dati.setUsname(username);//analogo al ramo di login
                //preparo la risposta per il client, la risposta ha lo stesso formato della richiesta
                lendati = "logout:".length();//lunghezza dei dati
                dati.allocAnswer(lendati + 8 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio e l'intero finale che indica lo stato dell operazione

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
                            else {OutWriter.writeInt(-3);} //-2 indica che l utente non ha inserito l'username corretto
                        }
                        else {OutWriter.writeInt(-2); } //-2 indica che l utente non è loggato
                    }
                    else OutWriter.writeInt(-1);// -1 indica utente non registrato

                    dati.SetAnswer(SupportOut.toByteArray());
                }
                catch (Exception e) {e.printStackTrace();}

                break;
            case "playWORDLE" ://caso in cui un utente richiede di iniziare una partita
                //_--------------------//


                //recupero il mutua esclusione i timestamps necessari per poter inviare al client
                //quando eventualemnete sarà generata la prossima parola
                ReadWordLock.lock();
                long lastW = Gioco.getLastTime();
                long currentW = Gioco.getCurrentTime();
                long nextW = Gioco.getNextTime();
                ReadWordLock.unlock();
                //metodo privato per effettuare il calcolo dell tempo di produzione della nuova parola
                long nextwClient = CalculateTime(currentW, nextW);

                //---------------------//


                username = Tok.nextToken(" ").replace(":", "");//recupero username
                dati.setUsname(username);//analogo al ramo di login

                lendati = "playWORDLE:".length();//lunghezza dei dati

                SupportOut = new ByteArrayOutputStream();
                try (DataOutputStream OutWriter = new DataOutputStream(SupportOut)){

                    ReadWordLock.lock();
                    int result = Gioco.setGame(username);
                    if(result == 0) {
                        dati.allocAnswer(lendati + 16 );//lunghezza dati + 4 byte per la lunghezza del messaggio + 4 byte l'intero finale per lo stato dell operazione + 8 byte per long
                        OutWriter.writeInt(lendati + 16);
                        OutWriter.writeChars("playWORDLE:");

                        OutWriter.writeInt(0);//0 indica che l utente puo cominciare a giocare
                        OutWriter.writeLong(nextwClient);
                    }
                    else if(result == 1) {
                        dati.allocAnswer(lendati + 8 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio e l'intero finale che indica lo stato dell operazione
                        OutWriter.writeInt(lendati + 8);
                        OutWriter.writeChars("playWORDLE:");

                        OutWriter.writeInt(-1);// -1 indica che l utente aveva gia chiesto un operazione di playWORDLE

                    }
                    else {

                        dati.allocAnswer(lendati + 8 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio e l'intero finale che indica lo stato dell operazione
                        OutWriter.writeInt(lendati + 8);
                        OutWriter.writeChars("playWORDLE:");

                        OutWriter.writeInt(-2);//-2 indica che l utente ha gia giocato al gioco
                    }

                    dati.SetAnswer(SupportOut.toByteArray());
                }
                catch (Exception e) {e.printStackTrace();}
                finally {ReadWordLock.unlock();}
                break;
            case "sendWord" :

                username = Tok.nextToken(" ").replace(":", "");//recupero username
                word = Tok.nextToken(" ");//recupero parola

                dati.setUsname(username);//inserisco l'username nel pacchetto in modo che il thread che
                                         //gestisce le connessioni possa effettuare il logout in caso
                                         //il client chiuda la conn all improvviso

                //Controllo che la parola sia presente nel vocabolario, in caso non ci sia ritorno un messaggio di errore
                if(CheckWord(word)) {
                    //caso in cui l utente non ha prima eseguito il comando playWORDLE oppure ha gia partecipato al gioco
                    //o vincendo la partita oppure esaurendo i tentativi per quella parola
                    int FlagResult = 0;
                    if((FlagResult = Gioco.Tentativo(username)) != 0) {

                        switch (FlagResult) {
                            case -1 ://caso in cui l utente non ha selezionato il comando playWORDLE

                                WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", -1, null);
                                break;
                            case -2 ://caso in cui l utente ha gia giocato e ha terminato i tentativi

                                WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", -2, null);
                                break;
                            case -3 ://ha vinto la partita precedentemente

                                WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", -3, null);
                                break;
                        }
                    }
                    else {
                        //recupero la parola del gioco in muta esclusione
                        ReadWordLock.lock();
                        String GameWord = Gioco.getWord();
                        ReadWordLock.unlock();

                        if(GameWord.equals(word)) {//caso in cui il client ha indovinato la parola
                            Gioco.setWinner(username);//setto i campi  per indicare che per quel utente la parola è stata indovinata
                            //devo inviare al client il messaggio di parola indovinata

                            WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", 0, null);//0 indica parola indovinata
                        }
                        else {
                            //a questo punto devo inviare i suggerimenti al client
                            // devo quindi effettuare il calcolo dei sugerimenti, produrre la risposta e inviarla
                            //1 indica che la risposta conterra i seuggerimenti
                            WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", 1, ComputeSuggestions(GameWord, word));
                        }
                    }
                }
                else {
                    WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", -4, null);//caso in cui la parola non esiste e il tentativo non viene considerato
                }
                break;
        }
    }


    //metodo usato per controllare che se la parola è presente nel vocabolario (in words)
    private boolean CheckWord(String GuessWord) {

        if(GuessWord.length() != 10) return false;
        for (int i = 0; i<Words.size(); i++) {
            if(Words.get(i).equals(GuessWord))return true;
        }
        return false;
    }
    private void WriteErrorOrWinOrSuggestionMessage(PkjData dati, String method, int error, String Suggestions) {


        if(Suggestions == null) {//caso in cui il metodo viene usato per inviare messaggi di errore o di vittoria
            int lendati = method.length();//lunghezza dei dati

            dati.allocAnswer(lendati + 8 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio e l'intero finale che indica lo stato dell operazione
            ByteArrayOutputStream SupportOut = new ByteArrayOutputStream();
            try (DataOutputStream OutWriter = new DataOutputStream(SupportOut)){

                OutWriter.writeInt(lendati + 8);
                OutWriter.writeChars(method);
                OutWriter.writeInt(error);//-1 indica che la parola non esiste e il tentativo non viene contato
                dati.SetAnswer(SupportOut.toByteArray());
            }
            catch (Exception e) {e.printStackTrace();}
        }
        else {//caso in cui il metodo viene usato per inviare suggerimenti
            int lendati = method.length() + Suggestions.length();//lunghezza dei dati

            dati.allocAnswer(lendati + 8 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio e l'intero finale che indica lo stato dell operazione
            ByteArrayOutputStream SupportOut = new ByteArrayOutputStream();
            try (DataOutputStream OutWriter = new DataOutputStream(SupportOut)){

                OutWriter.writeInt(lendati + 8);
                OutWriter.writeChars(method);
                OutWriter.writeInt(error);//-1 indica che la parola non esiste e il tentativo non viene contato
                OutWriter.writeChars(Suggestions);
                dati.SetAnswer(SupportOut.toByteArray());
            }
            catch (Exception e) {e.printStackTrace();}
        }
    }
    //metodo che costruisce i suggerimenti da inviare all utente
    private String ComputeSuggestions(String GameWord, String UsersWord) {

        //uso degli array di caratteri temporanei
        char [] GWchar = GameWord.toCharArray();
        char [] UWchar = UsersWord.toCharArray();
        char [] CharConsigli = new char[10];//array di caratteri per costruire i suggerimenti

        for(int i = 0; i<10; i++) {//vado avanti di 10 posizioni, sono sicuro che le parole sono lunghe 10 perche ho fatto i controlli precedentemente

            if(UWchar[i] == GWchar[i]) {//se i caratterio coincidono inserisco + nella stringa suggerimenti
                CharConsigli[i] = '+';
            }
            else {
                char [] trovata = {'0'};//uso un array di dimensione 1 per controllare se las lettera cercata è stata trovata
                //metodo ricorsivo che controlla se la lettera è presente
                CheckChars(UWchar, UWchar[i], GWchar, 0, i,10, CharConsigli, trovata);
                if(trovata[0] == '0') CharConsigli[i] = 'x';//se la lettera non è stata trovata inserisco il suggerimento che quella lettera
                                                            // non è presente nella parola del gioco
            }
        }
        //Stampe per i miei test, poi l elimino
        // String consigli = new String(CharConsigli);
        //System.out.println(consigli + "quiiii");
        //System.out.println("PAROLA DEL GIOCO " + GameWord);
        return new String(CharConsigli);
    }
    private void CheckChars(char [] UWchar, char Lettera, char [] GWchar, int idx, int idxlettera, int terminazione, char [] consigli, char [] trovata) {

        if(idx != terminazione) {//se la parola non è stata controllata tutta

            if(Lettera == GWchar[idx]) {//se la lettera presa in esame nel for che richiama il metodo è uguale a quella
                                        //presa in esame in questo momento della parola del gioco attuale
                trovata[0] = '1';       //setto il vettore di una dimensione per indicare che la lettera è stata trovata

                //controllo che all interno della parola che l utente ha inserito, nella posizione corrispondete alla posizione
                //in cui ho trovato la lettera nella parola del gioco presa in esame sia presente o meno la stessa lettera
                //in questo modo sono sicuro che la lettera è presente ma in posizione sbagliata
                if(GWchar[idx] != UWchar[idx]) {
                    //a questo punto modifico il vettore che corrisponde alla parola del gioco di questa sessione in modo
                    //da non considerarla piu
                    GWchar[idx] = '*';
                    consigli[idxlettera] = '?';
                }
                else {
                    //in questo caso invece devo ascorrere il resto della parola del gioco
                    CheckChars(UWchar, Lettera, GWchar, idx += 1, idxlettera, terminazione, consigli, trovata);
                }
            }
            else {//in questo caso invece vado avanti perche le lettere non sono uguali e quindi contiinuo a scorrere la parola del gioco
                CheckChars(UWchar, Lettera, GWchar, idx += 1, idxlettera, terminazione, consigli, trovata);
            }
        }
    }
    private long CalculateTime(long currentW, long nextW) {
        return (nextW - ( System.currentTimeMillis() - currentW));
    }
}
