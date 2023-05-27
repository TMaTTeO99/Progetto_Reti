import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerWordle{
    private static final int SIZE_SIZE = 4;//variabile per indicare il numeero dio byte di un int, serve per legere la dimensione
    private Registry RegistroRMI;
    private Registrazione Skeleton;
    private ExecutorService pool;
    private ConcurrentHashMap<String, Utente> Registrati; // Lista che conterrà gli utenti registrati
    private ImlementazioneRegistrazione ObjEsportato;//variabile per gestire la condition variable per il thread che scrive su json
    private LinkedBlockingDeque<String> DaSerializzare;//lista di supporto usata per capire quali utenti devono essere serializzati
                                                       //ad ogni iscrizione

    private HashMap<Integer, KeyData> LstDati;//HashmMap usata per recuperare i dati prodotti dai worker

    private SessioneWordle Game; //oggetto che rappresenta una sessione di gioco

    // utilizzo una lock e una cioondition variable perchè all'interno del costruttore lancio il thread che costruisce la
    // sessione periodica del gioco, a questo punto quindi devo poter recuperare l oggetto SessioneWordle, allora
    // devo effettuare la sincronizzazione fra il thread che esegue la classe ServerWordle e quello che esergue il metodo
    // run della classe OpenGame
    private ReentrantReadWriteLock RWlockWORD = new ReentrantReadWriteLock();
    private Lock ReadWordLock = RWlockWORD.readLock();
    private Lock WriteWordLock = RWlockWORD.writeLock();
    private ArrayList<String> Words;
    //private Condition CondGame = non so ancora se mi serve domani valuto

    //private ArrayList<SessioneWordle> GameQueue = new ArrayList<>();
    public ServerWordle(String PathJson , int Nthread, long TimeStempWord, int PortExport, long LTW, File ConfigureFile, ArrayList<String> Vocabolario) throws Exception{

        Game = new SessioneWordle();
        //lancio il thread che periodicamente creerà una nuova sessione di gioco
        Thread t = new Thread(new OpenGame(TimeStempWord, LTW, Vocabolario, ConfigureFile, Game, WriteWordLock/*, CondGame*/));
        t.start();
        Words = Vocabolario;
        //sezione da sincronizzare perche devo essere sicuro di recuperare l oggetto SessioneGame

        DaSerializzare = new LinkedBlockingDeque<>();
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Nthread);//pool di thread per eseguire i diversi task
        Registrati = new ConcurrentHashMap<>(); //creo il set degli utenti registrati con concurrenthashmap, per ora lascio i parametri di default

        ObjEsportato = new ImlementazioneRegistrazione(Registrati, DaSerializzare);//creo l' oggetto da esportare
        Skeleton = (Registrazione) UnicastRemoteObject.exportObject(ObjEsportato, 0);
        RegistroRMI = LocateRegistry.createRegistry(PortExport);
        RegistroRMI.bind("Registrazione", Skeleton);
        pool.execute(new MakeJson(Registrati, DaSerializzare, PathJson));//lancio il thread che effettua la serializzazione in background
        LstDati = new HashMap<>();


    }

    /**
     *  Metodo centrale del server per permettere il login e le future operazioni,
     *  Nota: per ora le info che dovranno poi essere raccolte dal file di config le
     *  definisco qui diretamente nel moetodo StartServer
     * */
    public void StartServer() {

        try (ServerSocketChannel AcceptSocket = ServerSocketChannel.open()) {//clausola try with resource per per chiudere la socket in caso di terminazione

            Selector selector = Selector.open();//istanza diuu Selector per poter fare multiplaxing delle connessioni
            AcceptSocket.bind(new InetSocketAddress(6501));//Porta Temporanea
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


                    if(ReadyKey.isAcceptable()) {//caso in un operazione di accept non ritorna null

                        //System.out.println("Ho accettato");
                        ServerSocketChannel ListenSocket = (ServerSocketChannel) ReadyKey.channel();//recupero la socket per accettare la connessione
                        SocketChannel channel = ListenSocket.accept();
                        channel.configureBlocking(false);//setto il channel come non bloccante
                        channel.register(selector, SelectionKey.OP_READ, ID_Channel);//registro il channel per operazione di lettura

                    }
                    else if (ReadyKey.isReadable()) {//caso in cui una operazione di read non ritorna 0

                        //System.out.println("readable");
                        //ATTENZIONE:::: considerare anche il problema della rejected exception del threadpool,

                        SocketChannel channel = (SocketChannel)ReadyKey.channel();
                        byte [] LenMexByte = new byte[SIZE_SIZE];
                        ByteBuffer LenMexBuffer = ByteBuffer.wrap(LenMexByte);


                        //nota: in questo caso in cui il client si sia sconnesso va effettuato il logout
                        // di tale client
                        if(channel.read(LenMexBuffer) == -1) {//leggo la len della richiesta e se non leggo nulla => il client ha chiuso la connessione
                                                           //=> cancello il channel dal selettore

                            //questo è il caso in cui il client ha chiuso la conn in modo bruto, a questo punto
                            //lato server devo effettuare il logout del client e eliminare lo stub dal
                            //client cosi da non inviare piu notifiche al client

                            KeyData dati = LstDati.get((Integer) ReadyKey.attachment());
                            if(dati != null) {
                                Future<PkjData> FuturePkj = dati.getDati();
                                if(FuturePkj.isDone()){
                                    PkjData Pkj = FuturePkj.get();
                                    Utente u = Registrati.get(Pkj.getUsname());
                                    if(u != null)u.setLogin(false);
                                }
                            }
                            ReadyKey.cancel();
                        }
                        else {
                            Future<PkjData> result = pool.submit(new Work(ReadyKey, selector, Registrati, (Integer) ReadyKey.attachment(), new PkjData(),  LenMexBuffer, Words, Game, ReadWordLock));
                            LstDati.put((Integer) ReadyKey.attachment(), new KeyData(ReadyKey, result));
                            ReadyKey.interestOps(SelectionKey.OP_WRITE);
                        }

                    }
                    else if(ReadyKey.isWritable()) {//caso in cui una operazione di write non ritorna 0

                        //System.out.println("wriatable");
                        SocketChannel channel = (SocketChannel) ReadyKey.channel();//recupero il canale
                        Future<PkjData> DataFuture = LstDati.get((Integer) ReadyKey.attachment()).getDati();


                        if(DataFuture.isDone()) {//se l'oggetto future è stato completato

                            PkjData dati = DataFuture.get();
                            try {//provo a scrivere i dati
                                System.out.println("Scrivo");
                                if(channel.write(ByteBuffer.wrap(dati.getAnswer())) == dati.getIdxAnswer()) {
                                    // Per ora commento questo LstDati.remove(ReadyKey.attachment());//rimuovo dalla struttura dati le informazioni
                                    ReadyKey.interestOps(SelectionKey.OP_READ);
                                }
                            }
                            catch (Exception e) {//se viene sollevata un eccezione perche il client ha chiuso la connessione elimino il channel selettore
                                ReadyKey.cancel();
                            }
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
        Set<Map.Entry<String, Utente>> utenti = Registrati.entrySet();
        for (Map.Entry<String, Utente> u : utenti) {
            try {
                u.getValue().getStub().SendNotifica(-1);
            }
            catch (Exception e) {e.printStackTrace();}
        }
    }
    /**
     * Metodo per effettuare la chiusura del servizio RMI,
     * quando tale servizio viene chiuso il server viene spento
     */
    public void ShutDownRMI() throws Exception{

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

}
