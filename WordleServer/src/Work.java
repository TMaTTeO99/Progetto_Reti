import java.io.*;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;

public class Work implements Runnable {

    private SelectionKey Key;//key per recuperare il channel e l id associato
    private ConcurrentHashMap<String, Utente> Registrati;//utenti del gioco
    private PkjData Dati;//pachetto che conterra le risposte per il client
    private SessioneWordle Gioco;//sessione corrente del gioco
    private Lock ReadWordLock;//readlock usata per accedere in mutua esclusione alla sessione del gioco in lettura
    private Lock WriteWordLock;//writelock usata per accedere in mutua esclusione alla sessione del gioco in scrittura
    private ArrayList<String> Words;//parole del vocabolario
    private LinkedBlockingDeque<DataToSerialize<?>> DaSerializzare;//cosa usata per dire al thread che serializza quando e cosa serializzare
    private ArrayList<UserValoreClassifica> Classifica;
    private Lock ReadLockClassifica;//readlock per l istanza della classifica
    private Lock WriteLockClassifica;//writelock per l istanza della classifica
    private InetSocketAddress AddressMulticastClients;//InetSocketaddress usato per inviare ai client del gruppo multicast i tentativi
    private GetDataConfig dataConfig;//oggetto che contiene dati di configurazione
    private HashMap<UUID, String> SecurityKeys;//Hashmap che contiene le chiavi di sessione per la cifratura
    public Work(SelectionKey k, ConcurrentHashMap<String, Utente> R, PkjData dati, ArrayList<String> Vocabolario,
                SessioneWordle g, Lock RWLock, LinkedBlockingDeque<DataToSerialize<?>> daserializzare, ArrayList<UserValoreClassifica> Clss,
                Lock RDLock, Lock WRLock, String IPMulticast, int PortMulticast, GetDataConfig dataConf, HashMap<UUID, String> ScrtyKeys, Lock WWLock) {

        dataConfig = dataConf;
        Key = k;
        Registrati = R;
        Dati = dati;
        Gioco = g;
        ReadWordLock = RWLock;
        WriteWordLock = WWLock;
        Words = Vocabolario;
        DaSerializzare = daserializzare;
        Classifica = Clss;
        ReadLockClassifica = RDLock;
        WriteLockClassifica = WRLock;
        AddressMulticastClients = new InetSocketAddress(IPMulticast, PortMulticast);
        SecurityKeys = ScrtyKeys;

    }
    public void run() {

        SocketChannel channel = (SocketChannel) Key.channel();//recupero il channel

        try {
            //recupero il token e il metodo della richiesta
            StringTokenizer newTok = null;
            StringTokenizer Tok = new StringTokenizer(new String(Dati.getRequest(), StandardCharsets.UTF_16), ":");
            String Method = Tok.nextToken();//recupero l'operazione che il client ha richiesto


            switch (Method) {

                case "login" :
                    //decifro i dati, creo una stringa e poi uso un tokenizer per parsare la stringa e fare le operazioni
                    newTok = new StringTokenizer(GetDatiCifreati(("login:".length() * 2), Dati), ":");
                    LoginMethod(newTok, Dati);
                    break;
                case "logout" :

                    LogoutMethod(Tok, Dati);
                    break;
                case "playWORDLE" :

                    PlayWordleMethod(Tok, Dati);
                    break;
                case "sendWord" :
                    //analogo al caso della login
                    newTok = new StringTokenizer(GetDatiCifreati(("sendWord:".length() * 2), Dati), ":");
                    SendWordMethod(newTok, Dati);
                    break;
                case "sendMeStatistics":

                    SendStatisticsMethod(Tok, Dati);
                    break;
                case "share":

                    ShareMethod(Tok, Dati);
                    break;
                case "showMeRanking":

                    showRankingMethod(Tok, Dati);
                    break;
                case "TimeNextWord":

                    SendTimeWordMethod(Tok, Dati);
                    break;
                case "dataforkey"://in questo caso i dati non sono cifrati perche ancora si sta creando la key
                    //metodo usato per generare la chiave di sessione con l utente
                    SendAndRicevereSecurityData(Tok, Dati);
                    break;
            }

            //qui invio la risposta al client
            try {SendDataToClient(channel);}
            catch (Exception e) {e.printStackTrace();}
        }
        catch (Exception e) {

            e.printStackTrace();

            //caso in cui viene sollevata un eccezione in uno dei metodi che elaborano la risposta al client
            Write_No_Cipher(Dati, "", -10, "");
            try {SendDataToClient(channel);}
            catch (Exception ee) {ee.printStackTrace();}
        }

    }

    //Metodi privati usati per costruire la risposta corretta
    private void SendAndRicevereSecurityData(StringTokenizer Tok, PkjData dati) {

        BigInteger C = new BigInteger(Tok.nextToken(" ").replace(":", ""));//recupero il dato del client
        BigInteger P = new BigInteger(String.valueOf(dataConfig.getP()));//recupero l intero primo P
        BigInteger s = null;

        //calcolo S per il protocollo DH
        BigInteger S = SecurityClass.Compute_C(dataConfig.getG(), dataConfig.getP());
        try {

            s = SecurityClass.getSecret();//recupero il segreto del server riguardo questa connessione

            String keySecurity = C.modPow(s, P).toString(2);//calcolo la chiave di sessione
            while(keySecurity.length() < 16){keySecurity += '0';}//se la chiave è < 128 bit faccio pudding

            SecurityKeys.put((UUID) Key.attachment(), keySecurity);//inserisco la chiave in una HashMap indicizzata dall id associato alla connessione

            //recupero le info per inviare i dati e li invio
            String IDString = ((UUID) Key.attachment()).toString();

            String String_S = String.valueOf(S);
            int lendati = String_S.length() + IDString.length();//lunghezza dei dati
            dati.allocAnswer(lendati + 12 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio ,4 per l'intero finale che indica lo stato dell operazione e 4 per l ID del channel

            ByteArrayOutputStream SupportOut = new ByteArrayOutputStream();
            try (DataOutputStream OutWriter = new DataOutputStream(SupportOut)){

                OutWriter.writeInt(lendati + 8);
                OutWriter.writeInt(IDString.length());
                OutWriter.writeChars(IDString);//scrivo i dati
                OutWriter.writeInt(1);//scrivo l intero che indica il tipo di operazione
                OutWriter.writeInt(String_S.length());//scrivo lunghezza di eventuali dati aggiuntivi
                OutWriter.writeChars(String_S);//scrivo i dati
                dati.SetAnswer(SupportOut.toByteArray());//inserisco i byte scitti nell pacchetto dati da inviare
            }
            catch (Exception e){e.printStackTrace();}

        }
        catch (NullPointerException e) {
            e.printStackTrace();
            Write_No_Cipher(dati, "", 0, "");
        }
    }
    private void SendTimeWordMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        username = Tok.nextToken(" ").replace(":", "");//recupero username

        Utente u = Registrati.get(username);//recupero l utente

        if(u != null){

            try {

                u.getReadLock().lock();
                if(u.getLogin((UUID) Key.attachment())) {//controllo se l utente ha fatto il login

                    //recupero le info riguardo la creazione della parola
                    long currentW = -1;
                    long nextW = -1;

                    try {
                        //accedo in mutua esclusione alla sessione di gioco
                        ReadWordLock.lock();
                        currentW = Gioco.getCurrentTime();
                        nextW = Gioco.getNextTime();
                    }
                    finally {ReadWordLock.unlock();}

                    //metodo privato per effettuare il calcolo dell tempo di produzione della nuova parola
                    long nextwClient = CalculateTime(currentW, nextW);

                    Write_No_Cipher(dati, "", 0, Long.toString(nextwClient));//richiamo il metodo per la creazione della risposta

                }
                else Write_No_Cipher(dati, "", -1, "");
            }
            finally {u.getReadLock().unlock();}

        }
        else Write_No_Cipher(dati, "", -1, "");

    }
    private void showRankingMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        username = Tok.nextToken(" ").replace(":", "");//recupero username

        Utente u = Registrati.get(username);
        if(u != null) {

            try {

                u.getReadLock().lock();
                if(u.getLogin((UUID) Key.attachment())) {//controllo se l utente ha fatto il login

                    String answer = null;
                    try {
                        //accedo in mutua esclusione alla classifica
                        ReadLockClassifica.lock();
                        answer = new String();
                        for(int i = 0; i< Classifica.size(); i++) {//costruisco la risposta
                            UserValoreClassifica temp = Classifica.get(i);
                            answer = answer.concat((i+1) + "°: " + "USER: " + temp.getUsername() + " SCORE: " + temp.getScore() + "\n");
                        }
                    }
                    finally {ReadLockClassifica.unlock();}

                    Write_No_Cipher(dati, "", 0, answer);
                }
                else Write_No_Cipher(dati, "", -1, "");
            }
            finally {u.getReadLock().unlock();}

        }
        else Write_No_Cipher(dati, "", -1, "");

    }
    private void ShareMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;

        username = Tok.nextToken(" ").replace(":", "");//recupero username
        u = Registrati.get(username);//recupero l utente

        if(u != null) {

            try {

                u.getReadLock().lock();//sincronizzo per l accesso ai dati dell utente
                if(u.getLogin((UUID) Key.attachment())) {//controllo se l utente ha fatto il login

                    try {
                        ReadWordLock.lock();//ho bisogno di sincronizzare con il thread che genera il gioco
                        InfoSessioneUtente tmpInfo = Gioco.getInfoGameUtente(username);//recupero le info della partita dell utente

                        if(tmpInfo != null) {//se l utente ha partecipato al gioco

                            if((tmpInfo.getQuitGame() && tmpInfo.getTentativi() > 0) || tmpInfo.getTentativi() >= 12 || tmpInfo.getResultGame()) {//se l utente non sta partecipando al gioco

                                //qui scorro i tentativi dell utente e costruisco i dati da inviare sul gruppo multicast
                                //costruisco una stringa contenente tutte le sottostringhe che rappresentano i suggerimenti
                                String answer = new String();

                                ArrayList<String> Suggerimenti = tmpInfo.getTryWord();
                                answer = answer.concat(username);//inserisco l username dell utente che ha fatto i tentativi

                                //costruisco la risposta
                                for(int  i = 0; i<Suggerimenti.size(); i++) {answer = answer.concat(" " + Suggerimenti.get(i));}

                                //invio la stringa con un datagrampacket al gruppo multicast
                                try (DatagramSocket sock = new DatagramSocket()){

                                    byte [] byteAnswer = answer.getBytes();
                                    DatagramPacket pkj = new DatagramPacket(byteAnswer, 0, byteAnswer.length, AddressMulticastClients);
                                    sock.send(pkj);

                                    Write_No_Cipher(dati, "", 0, "");//avviso il client che l operazione è stata fatta
                                }
                                catch (Exception e) {e.printStackTrace();}
                            }
                            else {
                                Write_No_Cipher(dati, "", -2, "");//caso in cui l utente è ancora in gioco
                            }
                        }
                        else Write_No_Cipher(dati, "", -1, "");//caso in cui l utente non ha partecipato al gioco
                    }
                    finally {ReadWordLock.unlock();}
                }
                else Write_No_Cipher(dati, "", -3, "");//caso in cui l utente non ha effettuato il login
            }
            finally {u.getReadLock().unlock();}

        }
        else Write_No_Cipher(dati, "", -3, "");

    }
    private void SendStatisticsMethod(StringTokenizer Tok, PkjData dati) {

        int GameUtente = 0;
        String username = null;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;

        username = Tok.nextToken(" ").replace(":", "");//recupero username

        //devo inviare le statistiche dell ultimo gioco dell utente
        //nel caso in cui l utente abbia gia chiesto di partecipare a un nuovo gioco devo inviare le
        //statistiche con il numero di partite giocate -1, altrimenti gli devo mandare le statistiche cosi come sono

        //Recupero l utente
        u = Registrati.get(username);

        if(u != null) {

            try {

                u.getReadLock().lock();
                if(!u.getLogin((UUID) Key.attachment())) {//utente non ha effettuato il login
                    Write_No_Cipher(dati, "", -1, "");
                }
                else {Cipher_AND_Write(dati, "", 0, GetStatisticsUser(username, u));}
            }
            finally {u.getReadLock().unlock();}

        }
        else Write_No_Cipher(dati, "", -1, "");//se u non è registrato
    }
    private void SendWordMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        String word = null;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;


        //controllo i dati che ricevo dall utente
        try {
            username = Tok.nextToken(" ").replace(":", "");//recupero username
            word = Tok.nextToken(" ");//recupero parola
            u = Registrati.get(username);
        }
        catch (Exception e) {//caso in cui l utente non inserisce correttamente la parola
            Write_No_Cipher(dati, "", -6, "");
            return;
        }
        if(u != null) {

            try {
                u.getWriteLock().lock();
                if(u.getLogin((UUID) Key.attachment())) {//se l utente ha effettuato il login

                    try {

                        ReadWordLock.lock();
                        int FlagResult = 0;
                        if((FlagResult = Gioco.Tentativo(username)) != 0) {

                            switch (FlagResult) {
                                case -1 ://caso in cui l utente non ha selezionato il comando playWORDLE
                                    Write_No_Cipher(dati, "", -1, "");
                                    break;
                                case -2 ://caso in cui l utente ha gia giocato e ha terminato i tentativi
                                    Write_No_Cipher(dati, "", -2, "");
                                    break;
                                case -3 ://ha vinto la partita precedentemente
                                    Write_No_Cipher(dati, "", -3, "");
                                    break;
                            }
                        }
                        else {
                            if(CheckWord(word)) {//Controllo che la parola sia presente nel vocabolario, in caso non ci sia ritorno un messaggio di errore

                                String GameWord = Gioco.getWord();
                                String wordTradotta = Gioco.getTranslatedWord();


                                if(GameWord.equals(word)) {//caso in cui il client ha indovinato la parola

                                    Gioco.setWinner(username);//setto i campi  per indicare che per quel utente la parola è stata indovinata

                                    //recupero il numero di tentativi fatti dal giocatore per vinvere l attuale partita
                                    int tentativiAttuali = Gioco.gettentativiUtente(username);

                                    //Aggiorno il numero di tentativi totali delle partite vinte per poter fare il punteggio in classifica
                                    u.setWinTentativi(u.getWinTentativi() + tentativiAttuali);

                                    //aumento il numero di partite vinte dal utente
                                    u.increasesWinGame();

                                    //ricalcolo la percentuale di partite vinte
                                    u.UpdatePercWingame();

                                    String suggestions = ComputeSuggestions(GameWord, word);//costruisco i suggerimenti per l utente
                                    Gioco.getTentativi().get(username).getTryWord().add(suggestions);//aggiungo il suggerimento alla sessione dell utente

                                    //recupero i tentativi della partita
                                    int tentativiUtente = Gioco.gettentativiUtente(username);

                                    //ricalcolo la distribuzione
                                    u.setGuesDistribuition(tentativiUtente - 1, (u.getGuesDistribuition(tentativiUtente - 1) + 1));

                                    //aumento striscia positiva di vittorie
                                    u.updateLastConsecutive(true);

                                    //aggiorno la classifica
                                    updateClassifica(username, u, u.getWinTentativi());

                                    //segnalo al thread che serializza i dati che un altro utente ha modificato le sue statistiche
                                    SendSerialization('U');

                                    //Invio la classifica al thread che serializza per salvare la classifica aggiornata
                                    SendSerialization('C');

                                    //invio al thread che serializza la sessione del gioco
                                    SendSerialization('I');

                                    //Costruisco il messaggio di parola indovinata
                                    Cipher_AND_Write(dati, GetStatisticsUser(username, u), 0, wordTradotta);//0 indica parola indovinata

                                }
                                else {
                                    //a questo punto devo inviare i suggerimenti al client se dopo quest ultimo tentativo ne ha almeno un altro
                                    //devo quindi effettuare il calcolo dei sugerimenti, produrre la risposta e inviarla

                                    String suggestions = ComputeSuggestions(GameWord, word);//costruisco i suggerimenti per l utente
                                    Gioco.getTentativi().get(username).getTryWord().add(suggestions);//aggiungo il tentativo alla sessione dell utente
                                    if(Gioco.gettentativiUtente(username) < 12) {
                                        Cipher_AND_Write(dati, "", 1, suggestions);//rispondo al client
                                    }
                                    else {//se invece il client ha terminato i tentativi invio al client la traduzione della parola e serializzare la sessione di Game

                                        SendSerialization('I');//serializzo l istanza del game in questo modo non perdo dati riguado la sconfitta del client
                                        u.updateLastConsecutive(false);//aggionro la striscia positiva di vittorie
                                        u.UpdatePercWingame();//ricalcolo la percentuale di partite vinte

                                        Cipher_AND_Write(dati, GetStatisticsUser(username, u), 2, wordTradotta);
                                    }
                                }
                            }
                            else {
                                Write_No_Cipher(dati, "", -4, "");//caso in cui la parola non esiste e il tentativo non viene considerato
                            }
                        }
                    }
                    finally {ReadWordLock.unlock();}

                }
                else Write_No_Cipher(dati, "", -5, "");
            }
            finally {u.getWriteLock().unlock();}
        }
        else Write_No_Cipher(dati, "", -5, "");

    }
    private void PlayWordleMethod(StringTokenizer Tok, PkjData dati) {

        int error = Integer.MAX_VALUE;
        String username = null;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;

        //recupero il mutua esclusione i timestamps necessari per poter inviare al client
        //quando eventualemnete sarà generata la prossima parola
        username = Tok.nextToken(" ").replace(":", "");//recupero username

        //devo controllare che il client abbia fatto il login
        u = Registrati.get(username);

        if(u != null) {

            try {
                u.getWriteLock().lock();
                if(u.getLogin((UUID) Key.attachment())) {//controllo che l utente abbia effettuato il login

                    //setto i campi per indicare che il client partecipa al game
                    int result = -10; //valore fittizio
                    try {
                        ReadWordLock.lock();
                        result = Gioco.setGame(username);
                    }
                    finally {ReadWordLock.unlock();}

                    if(result == 0) {

                        u.increasesGame();//aumento il numero di partite in cui il client ha partecipato

                        //inserisco in coda il messaggio per dire al thread che serializza che un utente ha aggiornato i suoi dati
                        SendSerialization('U');
                        error = 0;
                    }
                    else if(result == 1) {error = -1;}
                    else {error = -2;}
                }
            }
            finally {u.getWriteLock().unlock();}
        }
        else {error = -3;}
        Write_No_Cipher(dati, "", error, "");

    }
    private void LogoutMethod(StringTokenizer Tok, PkjData dati) {

        int error = Integer.MAX_VALUE;
        String username = null;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;


        try {
            username = Tok.nextToken(" ").replace(":", "");//recupero username
        }
        catch (Exception e){
            Write_No_Cipher(dati, "", -6, "");
            return;
        }
        if((u = Registrati.get(username)) != null) {//controllo che l utente sia registrato

            try {

                u.getWriteLock().lock();
                //controllo che l username associato a quella connessione per il login sia != null e che
                //coincida con quello che l utente ha inserito per il logout
                //in questo modo posso evitare che l utente che sta chiedendo di fare il logout butti fuori
                //dal gioco un altro utente
                if(u.getUserLogin((UUID) Key.attachment()) != null && u.getUserLogin((UUID) Key.attachment()).equals(username)) {

                    if(u.getLogin((UUID) Key.attachment())) {//controllo abbia effettuato il login

                        u.setLogin((UUID) Key.attachment(), false);//setto i campi che indicano che l utente non è piu loggato
                        error = 0;

                        //prima di settare l abbandono del gioco devo controllare se l utente ha provato a partecipare
                        if(Gioco.IsInGame(username)) {
                            Gioco.SetQuitUtente(username);//setto i campi che indicano che l utente ha abbandonato la partita
                            u.UpdatePercWingame();//aggiorno la percentuale di partite vinte
                            u.updateLastConsecutive(false);
                            SendSerialization('I');
                        }
                    }
                    else {error = -2;} //-2 indica che l utente non è loggato
                }
                else {error = -3;} //-3 indica che l utente non ha inserito l'username corretto
            }
            finally {u.getWriteLock().unlock();}

        }
        else error = -1;;// -1 indica utente non registrato

        Write_No_Cipher(dati, "", error, "");
    }
    private void LoginMethod(StringTokenizer Tok, PkjData dati) throws Exception{

        String username = null;
        String passwd = null;
        Utente u = null;
        int error = Integer.MAX_VALUE;//MAX_VALUE valore di inizializzazione

        try {//Effettuo un controllo preliminare su i dati che ricevo
            username = Tok.nextToken(" ").replace(":", "");//recupero username
            passwd = Tok.nextToken(" ");//recupero passwd
        }
        catch (Exception e) {
            Write_No_Cipher(dati, "", -6, "");
            return;
        }

        if((u = Registrati.get(username)) != null) {//controllo che l utente sia registrato

                //qui per effettuare il login controllo che immagine hash della password fornita
                //dall utente sia uguale a quella presente dell oggetto utente

                if(CheckImageHash(passwd, u)) {//controllo la password

                    try {
                        u.getWriteLock().lock();
                        u.setLogin((UUID) Key.attachment(), true);//setto i campi per il login
                        error = 0;
                    }
                    finally {u.getWriteLock().unlock();}

                }
                else {error = -2;}//-2 indica che l utente non ha inserito correttamente la passwd

        }
        else {error = -1;}// -1 indica utente non registrato

        Write_No_Cipher(dati, "", error, "");
    }

    //metodi privati per la gestione e il completamento delle richieste

    //metodo usato per controllare che se la parola è presente nel vocabolario (in words)
    private boolean CheckWord(String GuessWord) {//metodo usato per controllare che la parola sia presente nel vocabolario

        if(GuessWord.length() != 10) return false;
        for (int i = 0; i<Words.size(); i++) {
            if(Words.get(i).equals(GuessWord))return true;
        }
        return false;
    }
    //metodo privato usarto per comunicare con il thread che serializza
    private void SendSerialization(char type) {//metodo usato per comunicare con il thread che serializza

        try {

            switch (type) {
                //caso in cui un utente ha aggiornato le sue statistiche
                case 'U' :  DaSerializzare.put(new DataToSerialize<>(null, 'U'));
                    break;
                //caso in cui invio la classifica perche è stata aggiornata
                case 'C' : DaSerializzare.put(new DataToSerialize<>(Classifica, 'C'));
                    break;
                case 'I' :
                    //Anche se uso una blockingqueue ho necessita di usare la readlock per poter accedere all oggetto game in quanto
                    //in qualsiasi momento tale oggetto puo essere aggiornato dal thread che ricrea una nuova sessione di gioco
                    ReadWordLock.lock();
                        try {DaSerializzare.put(new DataToSerialize<>(Gioco, 'I'));}
                        catch (Exception e) {e.printStackTrace();}
                    ReadWordLock.unlock();
                    break;
            }

        }
        catch (Exception e) {e.printStackTrace();}
    }
    private void updateClassifica(String username, Utente tmpu, int Wintentativi) {

        //acquisisco la mutuia esclusione sulla classifica
        try {
            WriteLockClassifica.lock();

            //Prima di aggiornare la classifica salvo i nomi degli utenti che si trovano nelle prime 3 posizioni
            String user1 = null, user2 = null, user3 = null;

            int sizeClassifica = Classifica.size();
            switch (sizeClassifica) {
                case 1 :
                    //in caso in classifica ci sia un solo utente non devo fare nulla
                    //la classifica rimane invariata per quanto riguarda le posizioni
                    //richiamo il metodo sort per aggiornare i dati dell utente ma non
                    //devo inviare notifiche a nessuno perche le posizioni non variano
                    sortClassifica(username, tmpu, Wintentativi, sizeClassifica);
                    break;
                case 2:
                    user1 = Classifica.get(0).getUsername();
                    user2 = Classifica.get(1).getUsername();
                    sortClassifica(username, tmpu, Wintentativi, sizeClassifica);//aggiorno i dati e ordino la classifica

                    //nel caso siano cambiate la prima o la seconda posizione
                    if(!user1.equals(Classifica.get(0).getUsername()) || !user2.equals(Classifica.get(1).getUsername())) {sendNotify(sizeClassifica);}
                    break;
                default :
                    user1 = Classifica.get(0).getUsername();
                    user2 = Classifica.get(1).getUsername();
                    user3 = Classifica.get(2).getUsername();
                    sortClassifica(username, tmpu, Wintentativi, sizeClassifica);//ordino la classifica

                    //nel caso siano cambiate la prima o la seconda o la terza posizione
                    if((!user1.equals(Classifica.get(0).getUsername()) ||
                            !user2.equals(Classifica.get(1).getUsername()) ||
                            !user3.equals(Classifica.get(2).getUsername()))) {sendNotify(3);}//invio i primi 3 utenti (il podio)
                    break;
            }
        }
        finally {WriteLockClassifica.unlock();}

    }
    private void sortClassifica(String username, Utente tmpu, int Wintentativi, int sizeClassifica) {

        for(int i = 0; i<sizeClassifica; i++) {

            //ricerco l utente del quale devo aggiornare lo score
            UserValoreClassifica temp = Classifica.get(i);
            if(temp.getUsername().equals(username)) {
                temp.UpdateSCore(tmpu.getWinGame(), Wintentativi);
                Collections.sort(Classifica);
                break;//esco dal ciclo
            }
        }
    }
    //metodo usato per inviare le notifiche ai client riguardo la variazione della classifica
    private void sendNotify(int Dim) {

        ArrayList<UserValoreClassifica> ClassificaNotifiche = new ArrayList<>();

        try {
            ReadLockClassifica.lock();
            for(int i = 0; i<Dim; i++) {ClassificaNotifiche.add(Classifica.get(i));}
        }
        finally {ReadLockClassifica.unlock();}

        for(Utente u : Registrati.values()) {

            try {
                u.getReadLock().lock();
                HashMap<UUID, InfoLogin> MapStub = u.getLoginChannel();

                for(InfoLogin i : MapStub.values()) {

                    NotificaClient stub = i.getStub();
                    if(stub != null) {
                        try {stub.SendNotifica(ClassificaNotifiche);}
                        catch (Exception e){e.printStackTrace();}
                    }
                }
            }
            finally {u.getReadLock().unlock();}
        }
    }
    //metodo usato per scrivere la risposta che non ha bisogno di essere cifrata
    private void Write_No_Cipher(PkjData dati, String method, int error, String Other) {

        int lendati = method.length() + Other.length();//lunghezza dei dati
        dati.allocAnswer(lendati + 12);
        ByteArrayOutputStream SupportOut = new ByteArrayOutputStream();

        try (DataOutputStream OutWriter = new DataOutputStream(SupportOut)){

            OutWriter.writeInt(lendati + 8);
            OutWriter.writeChars(method);//nome dell eventiale nome del metodo anche se non usato (tipicamente stringa vuota)
            OutWriter.writeInt(error);//scrivo l intero che indica il tipo di operazione
            OutWriter.writeInt(Other.length());//scrivo lunghezza di eventuali dati aggiuntivi
            OutWriter.writeChars(Other);//scrivo i dati
            dati.SetAnswer(SupportOut.toByteArray());//inserisco i byte scitti nell pacchetto dati da inviare

        }
        catch (Exception e) {e.printStackTrace();}

    }
    //metodo usato per scrivere la risposta che ha bisogno di essere cifrata
    private void Cipher_AND_Write(PkjData dati, String method, int error, String Other) {


        int lendati = method.length() + Other.length();//lunghezza dei dati
        dati.allocAnswer(lendati + 12 );//lunghezza dei dati + 4 byte per contenere la lunghezza del messaggio e 4 per l'intero finale che indica lo stato dell operazione

        ByteArrayOutputStream SupportOut = new ByteArrayOutputStream();

        try (DataOutputStream OutWriter = new DataOutputStream(SupportOut)){

            String KeySecurity = SecurityKeys.get((UUID) Key.attachment());
            byte [] datiCifrati = SecurityClass.encrypt(Other + method, KeySecurity);

            OutWriter.writeInt(datiCifrati.length + 8);
            OutWriter.writeInt(error);
            OutWriter.writeInt(datiCifrati.length);
            OutWriter.write(datiCifrati, 0, datiCifrati.length);
            dati.SetAnswer(SupportOut.toByteArray());//inserisco i byte scitti nell pacchetto dati da inviare

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
        //stampa di test
        System.out.println("PAROLA DEL GIOCO " + GameWord);//per ora decommento cosi vedo la parola
        return new String(CharConsigli);
    }
    //metodo usato per recuperare i dati di un utente e costruire una stringa contenente le sue statistiche
    private String GetStatisticsUser(String username, Utente u) {

        int GameUtente = 0;
        try {
            ReadWordLock.lock();
            //controllo se l utente in questo momento sta giocando
            if(Gioco.IsInGame(username)){
                GameUtente = u.getGame() - 1;
            }
            else GameUtente = u.getGame();
        }
        finally {ReadWordLock.unlock();}

        //recupero le statistiche dell utente e creo la stringa che deve essere inviata
        String answer = new String(new byte[0], StandardCharsets.UTF_8);

        answer = answer.concat("Partite giocate == " + Integer.toString(GameUtente) + "\n");
        answer = answer.concat("Partite vinte == " + Integer.toString(u.getWinGame()) + "\n");
        answer = answer.concat("Percentuale partite vinte == " +Float.toString(u.getWinGamePerc()) + "\n");
        answer = answer.concat("Striscia positiva == " + Integer.toString(u.getLastConsecutive()) + "\n");
        answer = answer.concat("Massima striscia positiva == " + Integer.toString(u.getMaxConsecutive()) + "\n");

        int [] TmpGuessDistrib = u.getGuesDistribuition();
        for(int i = 0; i<12; i++) {answer = answer.concat("Vinte in " + (i+1) + " tentativi == " + Integer.toString(TmpGuessDistrib[i]) + "\n");}

        return answer;
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
    //metodo usato per rappresentare la cifratura della password fatta con SHA-256 in una stringa esadecimale
    private String bytesToHex(byte[] hash) {

        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);//recupero  il byte senza il segno
            if(hex.length() == 1) {//se hex ha lunghezza 1 aggingo uno 0 per avere una rappresentazione esadecimale con 2 cifre
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    //metodi usato per controllare la password che l utente ha fornito nella login
    private boolean CheckImageHash(String Originalpass, Utente u) throws Exception{

        try {
            u.getReadLock().lock();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");//recupo la funzione HASH
            byte[] encodedhash = digest.digest(Originalpass.getBytes(StandardCharsets.UTF_8));
            return u.getPassswd().equals(bytesToHex(encodedhash));
        }
        finally {u.getReadLock().unlock();}

    }
    //metodo usato per la decifrazione dei dati
    private String GetDatiCifreati(int position, PkjData Dati) {

        String securityKey = SecurityKeys.get((UUID) Key.attachment());//recupero la chiave di sessione
        byte [] req = new byte[Dati.getRequest().length - position];//uso un array di supporto

        //copio i dati nell array di supporto che contiene i dati cifrati
        System.arraycopy(Dati.getRequest(), position, req, 0, Dati.getRequest().length - position);

        //decifro i dati
        return SecurityClass.decrypt(req, securityKey);

    }
    //metodo che effettivamente invia i dati all utente dopo che sono stati creati
    private void SendDataToClient(SocketChannel channel) throws Exception {

        int flag = 0;//flag per far terminare la scrittura dei dati nel caso il client chiuda la connessione anche se verra sollevata una eccezione
        while((flag = channel.write(ByteBuffer.wrap(Dati.getAnswer()))) != Dati.getIdxAnswer() && flag != -1);

    }
    //metodo usato per calcolare quando sarà prodotta la prossima parola quindi la prossima sessione di gioco
    private long CalculateTime(long currentW, long nextW) {
        return (nextW - ( System.currentTimeMillis() - currentW));
    }
}
