import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;

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
    private InetSocketAddress AddressMulticastClients;//InetSocketaddress usato per inviare ai client del gruppo multicast i tentativi
    public Work(SelectionKey k, ConcurrentHashMap<String, Utente> R, PkjData dati, ArrayList<String> Vocabolario,
                SessioneWordle g, Lock RWLock, LinkedBlockingDeque<DataToSerialize> daserializzare, ArrayList<UserValoreClassifica> Clss,
                Lock RDLock, Lock WRLock, String IPMulticast, int PortMulticast) {
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
        AddressMulticastClients = new InetSocketAddress(IPMulticast, PortMulticast);
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
            }
        }
        catch (Exception e) {e.printStackTrace();}

        //qui ora prima di chiudere invio i dati al client
        try {
            int flag = 0;//flag per far terminare la scrittura dei dati nel caso il client chiuda la connessione anche se verra sollevata una eccezione
            while((flag = channel.write(ByteBuffer.wrap(Dati.getAnswer()))) != Dati.getIdxAnswer() && flag != -1);
        }
        catch (Exception e) {e.printStackTrace();}
    }

    //Metodi privati usati per costruire la risposta corretta
    private void SendTimeWordMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        username = Tok.nextToken(" ").replace(":", "");//recupero username

        Utente u = Registrati.get(username);
        if(u != null && u.getLogin((Integer) Key.attachment())) {//controllo se l utente ha fatto il login

            ReadWordLock.lock();
              long currentW = Gioco.getCurrentTime();
              long nextW = Gioco.getNextTime();
            ReadWordLock.unlock();

            //metodo privato per effettuare il calcolo dell tempo di produzione della nuova parola
            long nextwClient = CalculateTime(currentW, nextW);

            WriteErrorOrWinOrSuggestionMessage(dati, "", 0, Long.toString(nextwClient));

        }
        else WriteErrorOrWinOrSuggestionMessage(dati, "", -1, "");

    }
    private void showRankingMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        username = Tok.nextToken(" ").replace(":", "");//recupero username

        Utente u = Registrati.get(username);
        if(u != null && u.getLogin((Integer) Key.attachment())) {//controllo se l utente ha fatto il login

            ReadLockClassifica.lock();
                String answer = new String();
                for(int i = 0; i< Classifica.size(); i++) {
                    UserValoreClassifica temp = Classifica.get(i);
                    answer = answer.concat("USER: " + temp.getUsername() + " SCORE: " + temp.getScore() + "\n");
                }
            ReadLockClassifica.unlock();
            WriteErrorOrWinOrSuggestionMessage(dati, "", 0, answer);
        }
        else WriteErrorOrWinOrSuggestionMessage(dati, "", -1, "");

    }
    private void ShareMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;

        username = Tok.nextToken(" ").replace(":", "");//recupero username
        u = Registrati.get(username);

        if(u != null && u.getLogin((Integer) Key.attachment())) {//controllo se l utente ha fatto il login

            InfoSessioneUtente tmpInfo = Gioco.getInfoGameUtente(username);
            if(tmpInfo != null) {//se l utente ha partecipato al gioco e l ha concluso

                if((tmpInfo.getQuitGame() && tmpInfo.getTentativi() > 0) || tmpInfo.getTentativi() >= 12 || tmpInfo.getResultGame()) {//se l utente non sta partecipando al gioco

                    //qui scorro i tentativi dell utente e costruisco i dati da inviare sul gruppo multicast
                    //costruisco una stringa contenente tutte le sottostringhe che rappresentano i suggerimenti
                    String answer = new String();

                    ArrayList<String> Suggerimenti = tmpInfo.getTryWord();
                    answer = answer.concat(username);

                    for(int  i = 0; i<Suggerimenti.size(); i++) {answer = answer.concat(" " + Suggerimenti.get(i));}

                    //invio la stringa con un datagrampacket al gruppo multicast
                    try (DatagramSocket sock = new DatagramSocket()){

                        byte [] byteAnswer = answer.getBytes();
                        DatagramPacket pkj = new DatagramPacket(byteAnswer, 0, byteAnswer.length, AddressMulticastClients);
                        sock.send(pkj);

                        WriteErrorOrWinOrSuggestionMessage(dati, "", 0, "");
                    }
                    catch (Exception e) {e.printStackTrace();}
                }
                else {
                    WriteErrorOrWinOrSuggestionMessage(dati, "", -2, "");//caso in cui l utente è ancora in gioco
                }
            }
            else WriteErrorOrWinOrSuggestionMessage(dati, "", -1, "");//caso in cui l utente non ha partecipato al gioco
        }
        else WriteErrorOrWinOrSuggestionMessage(dati, "", -3, "");//caso in cui l utente non ha effettuato il login

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

        //utente non ha effettuato il login
        if(u != null && !u.getLogin((Integer) Key.attachment())) {
            WriteErrorOrWinOrSuggestionMessage(dati, "", -1, "");
        }
        else {
            //controllo se l utente in questo momento sta giocando
            if(Gioco.IsInGame(username)){
                GameUtente = u.getGame() - 1;
            }
            else GameUtente = u.getGame();

            //recupero le statistiche dell utente e creo la stringa che deve essere inviata
            String answer = new String(new byte[0], StandardCharsets.UTF_8);

            answer = answer.concat("Partite giocate == " + Integer.toString(GameUtente) + "\n");
            answer = answer.concat("Partite vinte == " + Integer.toString(u.getWinGame()) + "\n");
            answer = answer.concat("Percentuale partite vinte == " +Float.toString(u.getWinGamePerc()) + "\n");
            answer = answer.concat("Striscia positiva == " + Integer.toString(u.getLastConsecutive()) + "\n");
            answer = answer.concat("Massima striscia positiva == " + Integer.toString(u.getMaxConsecutive()) + "\n");

            int [] TmpGuessDistrib = u.getGuesDistribuition();
            for(int i = 0; i<12; i++) {answer = answer.concat("Vinte in " + (i+1) + " tentativi == " + Integer.toString(TmpGuessDistrib[i]) + "\n");}

            System.out.println(answer);
            WriteErrorOrWinOrSuggestionMessage(dati, "", 0, answer);
        }
    }
    private void SendWordMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        String word = null;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;

        username = Tok.nextToken(" ").replace(":", "");//recupero username
        word = Tok.nextToken(" ");//recupero parola

        u = Registrati.get(username);

        if(u != null && u.getLogin((Integer) Key.attachment())) {//se l utente ha effettuato il login
            //caso in cui l utente non ha prima eseguito il comando playWORDLE oppure ha gia partecipato al gioco
            //o vincendo la partita oppure esaurendo i tentativi per quella parola
            int FlagResult = 0;
            if((FlagResult = Gioco.Tentativo(username)) != 0) {

                switch (FlagResult) {
                    case -1 ://caso in cui l utente non ha selezionato il comando playWORDLE
                        WriteErrorOrWinOrSuggestionMessage(dati, "", -1, "");
                        break;
                    case -2 ://caso in cui l utente ha gia giocato e ha terminato i tentativi
                        WriteErrorOrWinOrSuggestionMessage(dati, "", -2, "");
                        break;
                    case -3 ://ha vinto la partita precedentemente
                        WriteErrorOrWinOrSuggestionMessage(dati, "", -3, "");
                        break;
                }
            }
            else {
                if(CheckWord(word)) {//Controllo che la parola sia presente nel vocabolario, in caso non ci sia ritorno un messaggio di errore
                    //recupero la parola del gioco in muta esclusione
                    ReadWordLock.lock();
                        String GameWord = Gioco.getWord();
                        String wordTradotta = Gioco.getTranslatedWord();
                    ReadWordLock.unlock();

                    if(GameWord.equals(word)) {//caso in cui il client ha indovinato la parola

                        Gioco.setWinner(username);//setto i campi  per indicare che per quel utente la parola è stata indovinata

                        //recupero il numero di tentativi fatti dal giocatore per vinvere l attuale partita
                        int tentativiAttuali = Gioco.gettentativiUtente(username);

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
                        System.out.println((float) (u.getGuesDistribuition(tentativiUtente - 1) * 100) / (float) u.getWinGame());

                        //aumento striscia positiva di vittorie
                        u.updateLastConsecutive(true);

                        //aggiorno la classifica
                        updateClassifica(username, u, tentativiAttuali);

                        //segnalo al thread che serializza i dati che un altro utente ha modificato le sue statistiche
                        SendSerialization('U');

                        //Invio la classifica al thread che serializza per salvare la classifica aggiornata
                        SendSerialization('C');

                        //invio al thread che serializza la sessione del gioco
                        SendSerialization('I');

                        //Costruisco il messaggio di parola indovinata
                        WriteErrorOrWinOrSuggestionMessage(dati, "", 0, wordTradotta);//0 indica parola indovinata

                    }
                    else {
                        //a questo punto devo inviare i suggerimenti al client se dopo quest ultimo tentativo ne ha almeno un altro
                        //devo quindi effettuare il calcolo dei sugerimenti, produrre la risposta e inviarla

                        String suggestions = ComputeSuggestions(GameWord, word);//costruisco i suggerimenti per l utente
                        Gioco.getTentativi().get(username).getTryWord().add(suggestions);//aggiungo il tentativo alla sessione dell utente
                         if(Gioco.gettentativiUtente(username) < 12) {
                             WriteErrorOrWinOrSuggestionMessage(dati, "", 1, suggestions);//rispondo al client
                         }
                         else {//se invece il client ha terminato i tentativi invio al client la traduzione della parola e serializzare la sessione di Game

                             SendSerialization('I');//serializzo l istanza del game in questo modo non perdo dati riguado la sconfitta del client
                             u.updateLastConsecutive(false);//aggionro la striscia positiva di vittorie
                             u.UpdatePercWingame();//ricalcolo la percentuale di partite vinte

                             WriteErrorOrWinOrSuggestionMessage(dati, "", 2, wordTradotta);
                         }
                    }
                }
                else {
                    WriteErrorOrWinOrSuggestionMessage(dati, "", -4, "");//caso in cui la parola non esiste e il tentativo non viene considerato
                }
            }
        }
        else WriteErrorOrWinOrSuggestionMessage(dati, "", -5, "");
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

        if(u != null && u.getLogin((Integer) Key.attachment())) {//controllo che l utente abbia effettuato il login

            ReadWordLock.lock();
                int result = Gioco.setGame(username);
            ReadWordLock.unlock();

            if(result == 0) {

                u.increasesGame();
                //inserisco in coda il messaggio per dire al thread che serializza che un utente ha aggiornato i suoi dati
                SendSerialization('U');
                error = 0;
            }
            else if(result == 1) {
                error = -1;
            }
            else {
                error = -2;
            }
        }
        else {
            error = -3;
        }
        WriteErrorOrWinOrSuggestionMessage(dati, "", error, "");

    }
    private void LogoutMethod(StringTokenizer Tok, PkjData dati) {

        int error = Integer.MAX_VALUE;
        String username = null;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;

        username = Tok.nextToken(" ").replace(":", "");//recupero username

        if((u = Registrati.get(username)) != null) {

            if(u.getLogin((Integer) Key.attachment())) {

                if(u.getUserLogin((Integer) Key.attachment()) != null && u.getUserLogin((Integer) Key.attachment()).equals(username)) {


                    u.setLogin((Integer) Key.attachment(), false);
                    error = 0;

                    //prima di settare l abbandono del gioco devo controllare se l utente ha provato a partecipare
                    if(Gioco.IsInGame(username)) {
                        Gioco.SetQuitUtente(username);
                        u.UpdatePercWingame();//aggionro la percentuale di partite vinte
                        SendSerialization('I');
                    }
                }
                else {error = -3;} //-3 indica che l utente non ha inserito l'username corretto
            }
            else {error = -2; } //-2 indica che l utente non è loggato
        }
        else error = -1;;// -1 indica utente non registrato
        WriteErrorOrWinOrSuggestionMessage(dati, "", error, "");
    }
    private void LoginMethod(StringTokenizer Tok, PkjData dati) {

        String username = null;
        String passwd = null;
        ByteArrayOutputStream SupportOut = null;
        Utente u = null;
        int error = Integer.MAX_VALUE;
        //MAX_VALUE valore che non uso sicuramente per inviare i messaggi


        SupportOut = new ByteArrayOutputStream();
        try{
            username = Tok.nextToken(" ").replace(":", "");//recupero username
            passwd = Tok.nextToken(" ");//recupero passwd

            if((u = Registrati.get(username)) != null) {
                if(u.getPassswd().equals(passwd)) {

                    u.setLogin((Integer) Key.attachment(), true);
                    error = 0;
                }
                else error = -2;//-2 indica che l utente non ha inserito correttamente la passwd
            }
            else error = -1;// -1 indica utente non registrato
            WriteErrorOrWinOrSuggestionMessage(dati, "", error, "");
        }
        catch (Exception e) {//in caso venga sollevata un eccezione
            if(e instanceof NoSuchElementException) {//se viene sollevata perche l utente non ha inserito correttamente
                                                    //i dati
                WriteErrorOrWinOrSuggestionMessage(dati, "", -4, "");
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
    //metodo privato usarto per comunicare con il thread che serializza
    private void SendSerialization(char type) {

        try {

            switch (type) {
                case 'U' :  DaSerializzare.put(new DataToSerialize<>(null, 'U'));
                    break;
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
    private void sendNotify(int Dim) {

        ArrayList<UserValoreClassifica> ClassificaNotifiche = new ArrayList<>();

        for(int i = 0; i<Dim; i++) {ClassificaNotifiche.add(Classifica.get(i));}
        for(Utente u : Registrati.values()) {

            NotificaClient stub = u.getStub();
            if(stub != null) {
                try {stub.SendNotifica(ClassificaNotifiche);}
                catch (Exception e){e.printStackTrace();}
            }
        }
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
        //stampa di test
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
