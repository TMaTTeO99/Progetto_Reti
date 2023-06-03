import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Work implements Runnable {

    private SelectionKey Key;//variabile utilizzata per reperire e settare i dati per le comunicazioni come ad es il channell e l attach
    private ConcurrentHashMap<String, Utente> Registrati;
    private PkjData Dati;
    private SessioneWordle Gioco;
    private Lock ReadWordLock;
    private ArrayList<String> Words;
    private LinkedBlockingDeque<DataToSerialize> DaSerializzare;
    private ArrayList<UserValoreClassifica> Classifica;
    private Lock ReadLockClassifica;
    private Lock WriteLockClassifica;

    //ReadLockClassifica, WriteLockClassifca

    public Work(SelectionKey k, ConcurrentHashMap<String, Utente> R, PkjData dati, ArrayList<String> Vocabolario,
                SessioneWordle g, Lock RWLock, LinkedBlockingDeque<DataToSerialize> daserializzare, ArrayList<UserValoreClassifica> Clss,
                Lock RDLock, Lock WRLock) {
        Key = k;
        Registrati = R;
        Dati = dati;
        Gioco = g;
        ReadWordLock = RWLock;
        Words = Vocabolario;
        DaSerializzare = daserializzare;
        Classifica = Clss;
        ReadLockClassifica = RDLock;
        WriteLockClassifica = WRLock;
    }
    public void run() {

        //per implementare la soluzione non bloccante devo per prima cosa recuperare l attached e il channel

        SocketChannel channel = (SocketChannel) Key.channel();//recupero il channel

        try {

            //recupero il token e il metodo della richiesta
            StringTokenizer Tok = new StringTokenizer(new String(Dati.getRequest(), StandardCharsets.UTF_16), ":");
            String Method = Tok.nextToken();//recupero l'operazione che il client ha richiesto

            switch (Method) {

                case "login" :
                    LoginMethod(Tok, Dati);
                    break;
                case "logout" :
                    LogoutMethod(Tok, Dati);
                    break;
                case "playWORDLE" :
                    PlayWordleMethod(Tok, Dati);
                    break;
                case "sendWord" :
                    SendWordMethod(Tok, Dati);
                    break;
            }
        }
        catch (Exception e) {e.printStackTrace();}

        //qui ora prima di chiudere invio i dati al client
        try {
            int flag = 0;//fal per far terminare la scrittura dei dati nel caso il client chiuda la connessione anche se verra sollevata una eccezione
            while((flag = channel.write(ByteBuffer.wrap(Dati.getAnswer()))) != Dati.getIdxAnswer() && flag != -1);
        }
        catch (Exception e) {e.printStackTrace();}
    }

    //Metodi privati usati per costruire la risposta corretta
    public void SendWordMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        String passwd = null;
        String word = null;
        int lendati = 0;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;

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

                        WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", -1, "");
                        break;
                    case -2 ://caso in cui l utente ha gia giocato e ha terminato i tentativi

                        //qui prima di inviare il messaggio devo interrompere la striscia positiva di partite vinte
                        Registrati.get(username).updateLastConsecutive(false);
                        WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", -2, "");
                        break;
                    case -3 ://ha vinto la partita precedentemente

                        WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", -3, "");
                        break;
                }
            }
            else {
                //recupero la parola del gioco in muta esclusione
                ReadWordLock.lock();
                    String GameWord = Gioco.getWord();
                    String wordTradotta = Gioco.getTranslatedWord();
                ReadWordLock.unlock();


                if(GameWord.equals(word)) {//caso in cui il client ha indovinato la parola

                    Gioco.setWinner(username);//setto i campi  per indicare che per quel utente la parola è stata indovinata

                    //recupero il numero di tentativi fatti dal giocatore per vinvere l attuale partita
                    int tentativiAttuali = Gioco.gettentativiUtente(username);

                    Utente tmpu = Registrati.get(username);//recupero utente

                    //aumento il numero di partite vinte dal utente
                    tmpu.increasesWinGame();

                    //ricalcolo la percentuale di partite vinte
                    tmpu.UpdatePercWingame();

                    //recupero i tentativi della partita
                    int tentativiUtente = Gioco.gettentativiUtente(username);

                    //ricalcolo la distribuzione
                    tmpu.setGuesDistribuition(tentativiUtente - 1, (tmpu.getGuesDistribuition(tentativiUtente - 1) + 1));
                    System.out.println((float) (tmpu.getGuesDistribuition(tentativiUtente - 1) * 100) / (float) tmpu.getWinGame());

                    //aumento striscia positiva di vittorie
                    tmpu.updateLastConsecutive(true);

                    //aggiorno la classifica
                    updateClassifica(username, tmpu, tentativiAttuali);

                    try {DaSerializzare.put(new DataToSerialize<>(null, 'U'));}
                    catch (Exception e) {e.printStackTrace();}

                    //qui devo inserire la classifica per serializzarla sul file json vedo pero ora se funziona la roba prima
                    try {DaSerializzare.put(new DataToSerialize<>(Classifica, 'C'));}
                    catch (Exception e) {e.printStackTrace();}

                    //-------------------------------------------//

                        //faccio una stampa della classifica sembra funzionare
                        ReadLockClassifica.lock();
                    for(int i = 0; i<Classifica.size(); i++) {
                        UserValoreClassifica tmp = Classifica.get(i);
                        System.out.println(tmp.getUsername() + " : " + tmp.getScore());
                    }
                        ReadLockClassifica.unlock();

                    //-------------------------------------------//

                    //Costruisco il messaggio di parola indovinata
                    WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", 0, wordTradotta);//0 indica parola indovinata

                    //WORNINGGG:::::::::::
                    //Faccio una prova per serializzare la sessione del gioco:
                    ReadWordLock.lock();
                        try {DaSerializzare.put(new DataToSerialize<>(Gioco, 'I'));}
                        catch (Exception e) {e.printStackTrace();}
                    ReadWordLock.unlock();
                    //la serializzazione sembra funzionare, bisogna poi capire quando deve essere fatta
                    //quindi capire quando deve essere passato in coda il gioco
                }
                else {
                    //a questo punto devo inviare i suggerimenti al client se dopo quest ultimo tentativo ne ha almeno un altro
                    // devo quindi effettuare il calcolo dei sugerimenti, produrre la risposta e inviarla
                    if(Gioco.gettentativiUtente(username) < 12) {
                        WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", 1, ComputeSuggestions(GameWord, word));
                    }
                    else {//se invece il client ha terminato i tentativi invio al client la traduzione della parola
                        WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", 2, wordTradotta);
                    }
                }
            }
        }
        else {
            WriteErrorOrWinOrSuggestionMessage(dati, "sendWord:", -4, "");//caso in cui la parola non esiste e il tentativo non viene considerato
        }


    }
    public void PlayWordleMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        String passwd = null;
        String word = null;
        int lendati = 0, error = Integer.MAX_VALUE;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;

        //recupero il mutua esclusione i timestamps necessari per poter inviare al client
        //quando eventualemnete sarà generata la prossima parola
        username = Tok.nextToken(" ").replace(":", "");//recupero username

        ReadWordLock.lock();
            long currentW = Gioco.getCurrentTime();
            long nextW = Gioco.getNextTime();
            int result = Gioco.setGame(username);
        ReadWordLock.unlock();


        //metodo privato per effettuare il calcolo dell tempo di produzione della nuova parola
        long nextwClient = CalculateTime(currentW, nextW);

        if(result == 0) {

            Utente tmpUtente = Registrati.get(username);
            tmpUtente.increasesGame();

            //inserisco in coda il messaggio per dire al thread che serializza che un utente ha aggiornato i suoi dati
            try {DaSerializzare.put(new DataToSerialize<>(null, 'U'));}
            catch (Exception e) {e.printStackTrace();}

            error = 0;
        }
        else if(result == 1) {
                error = -1;
        }
        else {
            error = -2;
        }
        WriteErrorOrWinOrSuggestionMessage(dati, "playWORDLE:", error, Long.toString(nextwClient));


    }
    public void LogoutMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        String passwd = null;
        String word = null;
        int lendati = 0, error = Integer.MAX_VALUE;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;

        username = Tok.nextToken(" ").replace(":", "");//recupero username

        if((u = Registrati.get(username)) != null) {

            if(u.getLogin()) {

                if(u.getID_CHANNEL() == (Integer) Key.attachment()) {
                    u.setLogin(false);
                    Gioco.SetQuitUtente(username);
                    error = 0;
                }
                else {error = -3;} //-3 indica che l utente non ha inserito l'username corretto
            }
            else {error = -2; } //-2 indica che l utente non è loggato
        }
        else error = -1;;// -1 indica utente non registrato
        WriteErrorOrWinOrSuggestionMessage(dati, "logout:", error, "");
    }
    private void LoginMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        String passwd = null;
        String word = null;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;
        int lendati = 0, error = Integer.MAX_VALUE;
        //MAX_VALUE valore che non uso sicuramente per inviare i messaggi


        SupportOut = new ByteArrayOutputStream();
        try{
            username = Tok.nextToken(" ").replace(":", "");//recupero username
            passwd = Tok.nextToken(" ");//recupero passwd

            if((u = Registrati.get(username)) != null) {
                if(u.getPassswd().equals(passwd)) {

                    u.setLogin(true);
                    u.setID_CHANNEL((Integer) Key.attachment());
                    error = 0;
                }
                else error = -2;//-2 indica che l utente non ha inserito correttamente la passwd
            }
            else error = -1;// -1 indica utente non registrato
            WriteErrorOrWinOrSuggestionMessage(dati, "login:", error, "");
        }
        catch (Exception e) {//in caso venga sollevata un eccezione
            if(e instanceof NoSuchElementException) {//se viene sollevata perche l utente non ha inserito correttamente
                                                    //i dati
                WriteErrorOrWinOrSuggestionMessage(dati, "login:", -4, "");
            }
            else e.printStackTrace();
        }
    }

    //metodi privati per la gestione e il completamento delle richieste

    //metodo usato per controllare che se la parola è presente nel vocabolario (in words)
    private boolean CheckWord(String GuessWord) {

        if(GuessWord.length() != 10) return false;
        for (int i = 0; i<Words.size(); i++) {
            if(Words.get(i).equals(GuessWord))return true;
        }
        return false;
    }
    private void updateClassifica(String username, Utente tmpu, int Wintentativi) {

        //acquisisco la mutuia esclusione sulla classifica
        WriteLockClassifica.lock();
            for(int i = 0; i<Classifica.size(); i++) {

                //ricerco l utente del quale devo aggiornare lo score
                UserValoreClassifica temp = Classifica.get(i);
                if(temp.getUsername().equals(username)) {
                    temp.UpdateSCore(tmpu.getWinGame(), Wintentativi);
                    Collections.sort(Classifica);
                    break;//esco dal ciclo
                }
            }
        WriteLockClassifica.unlock();
    }
    private void WriteErrorOrWinOrSuggestionMessage(PkjData dati, String method, int error, String Other) {

        int lendati = method.length() + Other.length();//lunghezza dei dati
        dati.allocAnswer(lendati + 8 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio e l'intero finale che indica lo stato dell operazione
        ByteArrayOutputStream SupportOut = new ByteArrayOutputStream();

        try (DataOutputStream OutWriter = new DataOutputStream(SupportOut)){

            OutWriter.writeInt(lendati + 8);
            OutWriter.writeChars(method);
            OutWriter.writeInt(error);
            OutWriter.writeInt(Other.length());
            OutWriter.writeChars(Other);
            dati.SetAnswer(SupportOut.toByteArray());

        }
        catch (Exception e) {e.printStackTrace();}
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
        System.out.println("PAROLA DEL GIOCO " + GameWord);//per ora decommento cosi vedo la parola
        return new String(CharConsigli);
    }
    private void CheckChars(char [] UWchar, char Lettera, char [] GWchar, int idx, int idxlettera, int terminazione, char [] consigli, char [] trovata) {

        if(idx != terminazione) {//se la parola non è stata controllata tutta

            if(Lettera == GWchar[idx]) {//se la lettera presa in esame nel for che richiama il metodo è uguale a quella
                                        //presa in esame in questo momento della parola del gioco attuale

                //controllo che all interno della parola che l utente ha inserito, nella posizione corrispondete alla posizione
                //in cui ho trovato la lettera nella parola del gioco presa in esame sia presente o meno la stessa lettera
                //in questo modo sono sicuro che la lettera è presente ma in posizione sbagliata
                if(GWchar[idx] != UWchar[idx]) {
                    //modifico il vettore che corrisponde alla parola del gioco di questa sessione in modo
                    //da non considerarla piu la lettera
                    GWchar[idx] = '*';
                    consigli[idxlettera] = '?';
                    trovata[0] = '1';       //setto il vettore di una dimensione per indicare che la lettera è stata trovata
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
