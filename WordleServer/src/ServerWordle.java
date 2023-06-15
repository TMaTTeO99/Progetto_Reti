import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerWordle{
    private static final int SIZE_SIZE = 4;//variabile per indicare il numero dio byte di un int, serve per legere i dati che arrivano
    private Registry RegistroRMI;
    private Registrazione Skeleton;
    private ExecutorService pool;
    private ConcurrentHashMap<String, Utente> Registrati; // Lista che conterrà gli utenti registrati
    private ImlementazioneRegistrazione ObjEsportato;//variabile per gestire la condition variable per il thread che scrive su json
    private LinkedBlockingDeque<DataToSerialize> DaSerializzare;//lista di supporto usata per capire quali utenti devono essere serializzati
                                                       //ad ogni iscrizione
    private SessioneWordle Game; //oggetto che rappresenta una sessione di gioco

    //uso delle readwritelock per gestire la sincronizzazione fra i thread worker, il thread che crea una istanza
    //del deel gioco e il thread che serializza la sessione, usando la write lock per il thread che crea la sessione
    //uso la read lock per i thread che devono solo leggere (i worker e il thread che serializza)
    //quando deserializzo non le uso perche non potranno esserci thread che competono in quanto siamo nella fase di set-up
    private ReentrantReadWriteLock RWlockWORD = new ReentrantReadWriteLock();
    private Lock ReadWordLock = RWlockWORD.readLock();
    private Lock WriteWordLock = RWlockWORD.writeLock();
    private ArrayList<String> Words; //vaìriabile che manterrà le parole del vocabolario

    //uso thread separati perche il pool ha una dimensione di thread limitata
    private Thread threadGame;//thread usato per la creazione perodica di una nuova sessione di gioco
    private Thread threadSerialize;//thread usato per la serializzazione dei dati

    private String URLtranslate;//stringa in cui sarà contenuta l'URL del servizio di traduzione recuperato dal file config

    private ReentrantReadWriteLock LockClassifca = new ReentrantReadWriteLock();//lock usata per implementare la mutua esclusine sulla classifica

    private Lock ReadLockClassifica = LockClassifca.readLock();

    private Lock WriteLockClassifca = LockClassifca.writeLock();

    private ArrayList<UserValoreClassifica> Classifica; //oggetto che rappresenta la classifica

    //per i test li lascio cosi, poi devo recuperarli dal file di config
    private int PortMulticast, Port_Listening;
    private String IP_multicast;

    public ServerWordle(ArrayList<String> Vocabolario, GetDataConfig dataConfig) throws Exception{

        PortMulticast = dataConfig.getPort_Multicast();
        IP_multicast = dataConfig.getIP_Multicast();
        Port_Listening = dataConfig.getPort_ListeningSocket();

        Classifica = new ArrayList<>();
        Game = new SessioneWordle();
        Registrati = new ConcurrentHashMap<>(); //creo il set degli utenti registrati con concurrenthashmap, per ora lascio i parametri di default
        DaSerializzare = new LinkedBlockingDeque<>();

        //lancio i thread separati dal threadpool
        threadGame = new Thread(new OpenGame(dataConfig.getTimeStempWord(), dataConfig.getLastTimeWord(), Vocabolario, dataConfig.getConfigureFile(), Game, WriteWordLock, dataConfig.getURLtranslate(), DaSerializzare));
        threadSerialize = new Thread(new MakeJson(Registrati, DaSerializzare, dataConfig.getPathSerialization(), ReadWordLock, Game, Classifica, ReadLockClassifica));

        threadGame.start();
        threadSerialize.start();

        Words = Vocabolario;

        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(dataConfig.getMaxThread());//pool di thread per eseguire i diversi task

        ObjEsportato = new ImlementazioneRegistrazione(Registrati, DaSerializzare, Classifica, WriteLockClassifca, ReadLockClassifica);//creo l' oggetto da esportare
        Skeleton = (Registrazione) UnicastRemoteObject.exportObject(ObjEsportato, 0);
        RegistroRMI = LocateRegistry.createRegistry(dataConfig.getPortExport());
        RegistroRMI.bind("Registrazione", Skeleton);
        URLtranslate = dataConfig.getURLtranslate();

    }

    /**
     *  Metodo centrale del server per permettere il login e le future operazioni,
     *  Nota: per ora le info che dovranno poi essere raccolte dal file di config le
     *  definisco qui diretamente nel moetodo StartServer
     * */
    public void StartServer() {

        try (ServerSocketChannel AcceptSocket = ServerSocketChannel.open()) {//clausola try with resource per per chiudere la socket in caso di terminazione

            Selector selector = Selector.open();//istanza diuu Selector per poter fare multiplaxing delle connessioni
            AcceptSocket.bind(new InetSocketAddress(Port_Listening));//Porta Temporanea
            AcceptSocket.configureBlocking(false);//setto il canale come non bloccante
            AcceptSocket.register(selector, SelectionKey.OP_ACCEPT);//registro la ServerSocketChannel per operazione di Accept
            Integer ID_Channel = 0;//intero per identificare la connessione

            while(true) { // per ora lascio while(true), in un secondo momento userò una var per la gestione della terminazione

                selector.select();//attendo che sia possibile effettuare una operazione

                Set<SelectionKey> Keys = selector.selectedKeys();//recupero il set di key pronte per per una delle operazioni
                Iterator<SelectionKey> IteratorKey = Keys.iterator();

                while(IteratorKey.hasNext()) {//scorro su tutto l'iteratore

                    SelectionKey ReadyKey = IteratorKey.next();//recupero la chiave su cui è pronta l'operazione
                    IteratorKey.remove();//rimuvo la chiave dall iteratore per non avere "inconsistenza"


                    if(ReadyKey.isAcceptable() && ReadyKey.isValid()) {//caso in un operazione di accept non ritorna null

                        ServerSocketChannel ListenSocket = (ServerSocketChannel) ReadyKey.channel();//recupero la socket per accettare la connessione
                        SocketChannel channel = ListenSocket.accept();
                        channel.configureBlocking(false);//setto il channel come non bloccante
                        channel.register(selector, SelectionKey.OP_READ, ID_Channel);//registro il channel per operazione di lettura

                    }
                    else if (ReadyKey.isReadable() && ReadyKey.isValid()) {//caso in cui una operazione di read non ritorna 0

                        //ATTENZIONE:::: considerare anche il problema della rejected exception del threadpool,

                        PkjData dati = null;
                        if((dati = ReadRequest(ReadyKey)) != null) {//se la lettura della richiesta è andata a buon fine lancio i worker
                            pool.execute(new Work(ReadyKey, Registrati, dati, Words, Game, ReadWordLock, DaSerializzare,
                                                  Classifica, ReadLockClassifica, WriteLockClassifca, IP_multicast, PortMulticast));
                        }
                    }
                }
                ID_Channel++;
            }
        }
        catch (Exception e){e.printStackTrace();}

    }

    /**
     * Metodo temporaneo per vedere se la registrazione dei client funziona
     */
    public void PrintRegistrati() {
        Set<String> UtentiRegistrati = Registrati.keySet();
        for (String u : UtentiRegistrati) {
            System.out.println(u);
        }
    }
    /**
     * Metodo temporaneo che uso ora per testare se il servizio di notifica funziona, tale servizio dovra essere attivato
     * in seguito ad un login e disattivato in seguito ad una logout
     */
    public void sendNotifica() {
        /**
         * per ora invio la notifica agli utenti registrati, quando implemento il login verra usata una struttura dati
         * per tenere traccia degli utenti loggati => mandero la notifica per quegli utenti che sono loggati, quindi
         * a questo punto per ora non uso nemmeno la readlock per la lettura tanto poi dovro usare altre lock
         */
      /*  Set<Map.Entry<String, Utente>> utenti = Registrati.entrySet();
        for (Map.Entry<String, Utente> u : utenti) {
            try {
                u.getValue().getStub().SendNotifica(-1);
            }
            catch (Exception e) {e.printStackTrace();}
        }

       */
    }
    /**
     * Metodo per effettuare la chiusura del servizio RMI,
     * quando tale servizio viene chiuso il server viene spento
     */
    /*public void ShutDownRMI() throws Exception{

        Metodo da adattare alle modifiche effettuate

        sendNotifica();
        DaSerializzare.put("STOP_THREAD");
        RegistroRMI.unbind("Registrazione");
        UnicastRemoteObject.unexportObject(ObjEsportato, true);
        System.out.println("Chiusura servizsio RMI");//stampa di prova
        pool.shutdown();
        while(!pool.isTerminated()){
            pool.awaitTermination(60L, TimeUnit.MILLISECONDS);
        }
        PrintRegistrati();
    }

     */

    //metodo utilizzato per leggere i dati che arrivano dalla richiesta
    private PkjData ReadRequest(SelectionKey key) {

        SocketChannel channel = (SocketChannel)key.channel();//recupero il channel
        byte [] LenMexByte = new byte[SIZE_SIZE];//alloc un byte[] per contenere len dei dati che sono arrivati
        ByteBuffer LenMexBuffer = ByteBuffer.wrap(LenMexByte);//uso un ByteBuffer per poter leggere tale dimensione
        PkjData Dati = new PkjData();//creo un pacchetto dati che conterrà la richiesta
        int flag = 0;//flag per controllare che mentre leggo i dati il client nnon chiudi la connessione


        //controllo che il client non abbia chiuso la connessione per un qualsiasi motivo
        try {
            //se il client ha chiuso la connessione cancello la chiave dal selettore
            if(channel.read(LenMexBuffer) == -1) {
                key.cancel();
                return null;//ritorno null quando il client ha chiuso la connessione e quindi non devo produrre risposte
            }
            //altrimenti recupero i dati
            LenMexBuffer.flip();//resetto position per poter leggere dal ByteBuffer
            Dati.allocRequest(LenMexBuffer.getInt());//alloco il vettore che conterrà la richiesta

            ByteBuffer RequestBuff = ByteBuffer.wrap(Dati.getRequest());//uso un ByteBuffer per leggere la richiesta

            //leggo e controllo che durante la lettura il client non chiudi la connessione
            while((flag = channel.read(RequestBuff)) != Dati.getRequest().length && flag != -1);
            if(flag == -1) return null;//se il client chiude la connessione ritorno null
        }
        catch (Exception e) {e.printStackTrace();}

        return Dati;
    }
}
