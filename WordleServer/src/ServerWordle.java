import java.net.InetSocketAddress;
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
    private Registry RegistroRMI;//registro usato per esportare i metodi remoti
    private Registrazione Skeleton;//oggetto che verrà esportato per consentire la registrazione degli utenti conRMI
    private ExecutorService pool;//threadpool per rispondere alle richieste dei client
    private ConcurrentHashMap<String, Utente> Registrati; // Lista che conterrà gli utenti registrati
    private ImplementazioneRegistrazione ObjEsportato;//variabile per gestire la condition variable per il thread che scrive su json
    private LinkedBlockingDeque<DataToSerialize<?>> DaSerializzare;//lista di supporto usata per capire quali utenti devono essere serializzati
                                                                //ad ogni iscrizione
    private SessioneWordle Game; //oggetto che rappresenta una sessione di gioco

    //uso delle readwritelock per gestire la sincronizzazione fra i thread worker, il thread che crea una istanza
    //del gioco e il thread che serializza la sessione, usando la write lock per il thread che crea la sessione
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

    //lock usate analogamente a quelle per il game ma per la classifica
    private ReentrantReadWriteLock LockClassifca = new ReentrantReadWriteLock();//lock usata per implementare la mutua esclusine sulla classifica
    private Lock ReadLockClassifica = LockClassifca.readLock();
    private Lock WriteLockClassifca = LockClassifca.writeLock();
    private ArrayList<UserValoreClassifica> Classifica; //oggetto che rappresenta la classifica
    private int PortMulticast, Port_Listening;
    private String IP_multicast;
    private GetDataConfig dataConfig;//oggetto che conterrà i parametri di configurazione
    private HashMap<UUID, String> SecurityKeys = new HashMap<>();//struttura dati che conterrà le chiavi di sicurezza associate alle connessioni
    private HashMap<UUID, PkjData> ListPackages = new HashMap<>();//HashMap usata per contenere i pacchetti che contengono le richieste dei client
    public ServerWordle(ArrayList<String> Vocabolario, GetDataConfig dataConf) throws Exception{

        dataConfig = dataConf;
        PortMulticast = dataConfig.getPort_Multicast();
        IP_multicast = dataConfig.getIP_Multicast();
        Port_Listening = dataConfig.getPort_ListeningSocket();

        Classifica = new ArrayList<>();
        Game = new SessioneWordle();
        Registrati = new ConcurrentHashMap<>(); //struttura dati che conterrà gli utenti del gioco
        DaSerializzare = new LinkedBlockingDeque<>();

        //lancio i thread separati dal threadpool
        threadGame = new Thread(new OpenGame(dataConfig.getTimeStempWord(), dataConfig.getLastTimeWord(), Vocabolario, dataConfig.getConfigureFile(), Game, WriteWordLock, dataConfig.getURLtranslate(), DaSerializzare, Registrati));
        threadSerialize = new Thread(new MakeJson(Registrati, DaSerializzare, dataConfig.getPathSerialization(), ReadWordLock, Game, Classifica, ReadLockClassifica, dataConf.getAfterUpDate()));

        threadGame.start();
        threadSerialize.start();

        Words = Vocabolario;

        try {//uso la clausola try catch per per effettuare il controllo sull servizio di registrazione
            ObjEsportato = new ImplementazioneRegistrazione(Registrati, DaSerializzare, Classifica, WriteLockClassifca, ReadLockClassifica, SecurityKeys);//creo l' oggetto da esportare
            Skeleton = (Registrazione) UnicastRemoteObject.exportObject(ObjEsportato, 0);
            RegistroRMI = LocateRegistry.createRegistry(dataConfig.getPortExport());
            RegistroRMI.bind("Registrazione", Skeleton);
        }
        catch (Exception e){
            threadGame.interrupt();
            threadSerialize.interrupt();
            DaSerializzare.put(new DataToSerialize<>(null, 'S'));
            RegistroRMI.unbind("Registrazione");
            UnicastRemoteObject.unexportObject(ObjEsportato, true);
            throw new Exception();//lancio un eccezione che verra catturata dal main per evitare di avviare il metodo StartServer inutilmente
        }

        URLtranslate = dataConfig.getURLtranslate();
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(dataConfig.getMaxThread());//pool di thread per eseguire i diversi task

    }

    public void StartServer() throws Exception{

        try (ServerSocketChannel AcceptSocket = ServerSocketChannel.open()) {//clausola try with resource per per chiudere la socket in caso di terminazione

            Selector selector = Selector.open();//istanza di Selector per poter fare multiplaxing delle connessioni
            AcceptSocket.bind(new InetSocketAddress(Port_Listening));
            AcceptSocket.configureBlocking(false);//setto il canale come non bloccante
            AcceptSocket.register(selector, SelectionKey.OP_ACCEPT);//registro la ServerSocketChannel per operazione di Accept
            UUID ID_Channel = UUID.randomUUID();//variabile casuale per identificare la connessione

            while(true) {

                selector.select();//attendo che sia possibile effettuare una operazione

                Set<SelectionKey> Keys = selector.selectedKeys();//recupero il set di key pronte
                Iterator<SelectionKey> IteratorKey = Keys.iterator();

                while(IteratorKey.hasNext()) {//scorro su tutto l'iteratore

                    SelectionKey ReadyKey = IteratorKey.next();//recupero la chiave su cui è pronta l'operazione
                    IteratorKey.remove();//rimuvo la chiave dall iteratore


                    if(ReadyKey.isAcceptable() && ReadyKey.isValid()) {//se pronta una operazione di accept

                        ServerSocketChannel ListenSocket = (ServerSocketChannel) ReadyKey.channel();//recupero la socket per accettare la connessione
                        SocketChannel channel = ListenSocket.accept();
                        channel.configureBlocking(false);//setto il channel come non bloccante
                        channel.register(selector, SelectionKey.OP_READ, ID_Channel);//registro il channel per operazione di lettura
                        ID_Channel = UUID.randomUUID();;
                    }
                    else if (ReadyKey.isReadable() && ReadyKey.isValid()) {//caso in cui una operazione di read non ritorna 0

                        PkjData dati = null;
                        if((dati = ReadRequest(ReadyKey)) != null) {//se la lettura della richiesta è andata a buon fine ed è completa
                            pool.execute(new Work(ReadyKey, Registrati, dati, Words, Game, ReadWordLock, DaSerializzare,
                                                  Classifica, ReadLockClassifica, WriteLockClassifca, IP_multicast, PortMulticast,
                                                    dataConfig, SecurityKeys, WriteWordLock));
                        }
                    }
                }
            }
        }
        catch (Exception e){//in caso di eccezione termino il server

            System.out.println("Terminazione server in corso");

            RegistroRMI.unbind("Registrazione");
            UnicastRemoteObject.unexportObject(ObjEsportato, true);

            threadGame.interrupt();
            threadSerialize.interrupt();
            DaSerializzare.put(new DataToSerialize<>(null, 'S'));

            UnicastRemoteObject.unexportObject(ObjEsportato, true);
            while(!pool.isTerminated()) {
                pool.awaitTermination(10L, TimeUnit.MILLISECONDS);
            }
        }
    }

    //metodo utilizzato per leggere i dati che arrivano dalla richiesta
    private PkjData ReadRequest(SelectionKey key) {

        //controllo se esiste gia il pacchetto nella HashMap
        try {

            int flag = 0;
            PkjData TestDati = null;
            SocketChannel channel = (SocketChannel)key.channel();//recupero il channel

            if((TestDati = ListPackages.get((UUID) key.attachment())) == null) {//se il pacchetto associato alla connessione non è presente devo cominciare a costruirlo da zero


                TestDati = new PkjData();//creo un pacchetto dati che conterrà la richiesta


                //se il client ha chiuso la connessione cancello la chiave dal selettore e lo stub
                //ritorno null quando il client ha chiuso la connessione e quindi non devo produrre risposte
                if(!ReadCheckClose(channel, TestDati, key)) {return null;}
                if(!TestDati.getLenMexBuffer().hasRemaining()) {//caso in cui è stata letta tutta la len dei dati

                    //comincio la lettura dei dati della richiesta

                    TestDati.getLenMexBuffer().flip();//resetto position per poter leggere dal ByteBuffer
                    TestDati.allocRequest(TestDati.getLenMexBuffer().getInt());//alloco il vettore che conterrà la richiesta

                    //leggo e controllo che durante la lettura il client non chiudi la connessione

                    switch(ReadReceivedData(channel, TestDati, key)) {
                        case 0 :
                            return TestDati;
                        case 1 :
                            ListPackages.put((UUID) key.attachment(), TestDati);//inserisco i dati in lista
                            break;
                        default ://non faccio nulla
                            break;
                    }
                    return null;
                }
                else {//caso in cui non tutta la lunghezza dei dati in arrivo è stata letta

                    ListPackages.put((UUID) key.attachment(), TestDati);//inserisco i dati in lista
                    return null;
                }
            }
            else {//caso in cui il pacchetto associato alla connessioone è gia presente nella struttura dati

                if(TestDati.getLenMexBuffer().hasRemaining()) {//controllo se ha terminato di leggere la dimensione dei dati

                    //in caso ancora bisogna leggere la dim dei dati
                    ReadCheckClose(channel, TestDati, key);//in questo caso non controllo nulla tanto comunque non devo produrre risposte

                    return null;//ritorno null cosi alla prossima iterazione riproverà a leggere
                }
                else {//caso in cui ha terminato la lettura dei dati riguardo la dimensione

                    switch(ReadReceivedData(channel, TestDati, key)) {

                        case 0 :
                            return TestDati;
                        case 1 :
                            ListPackages.put((UUID) key.attachment(), TestDati);//inserisco i dati in lista
                            break;
                        default ://non faccio nulla
                            break;
                    }
                    return null;
                }
            }
        }
        catch (Exception e) {

            key.cancel();//cancello la chiave dal selettore
            Utente releasedUtente = searchUtente((UUID) key.attachment());//elimino lo stub
            if(releasedUtente != null) releasedUtente.getLoginChannel().get((UUID) key.attachment()).RemoveSTub();

            return null;
        }
    }
    private int ReadReceivedData(SocketChannel channel, PkjData TestDati, SelectionKey key) throws Exception{

        int flag = 0;

        flag = channel.read(TestDati.getRequestBuff());

        if(flag == TestDati.getRequest().length) {return 0;}
        else if(flag != -1 && flag < TestDati.getRequest().length) {return 1;}
        else if(flag == -1){

            key.cancel();//cancello la chiave dal selettore

            //devo prima cercare il client che ha chiuso la connessione
            Utente releasedUtente = searchUtente((UUID) key.attachment());
            if(releasedUtente != null) releasedUtente.getLoginChannel().get((UUID) key.attachment()).RemoveSTub();

        }
        return -1;
    }
    private boolean ReadCheckClose(SocketChannel channel, PkjData TestDati, SelectionKey key) throws Exception{

        if(channel.read(TestDati.getLenMexBuffer()) == -1) {//qui ora nel caso in cui un client chiada la connessione in modo brusco devo eliminare il suo stub

            key.cancel();//cancello la chiave dal selettore

            //devo prima cercare il client che ha chiuso la connessione
            Utente releasedUtente = searchUtente((UUID) key.attachment());
            if(releasedUtente != null) releasedUtente.getLoginChannel().get((UUID) key.attachment()).RemoveSTub();

            return false;
        }
        return true;
    }
    private Utente searchUtente(UUID ID) {

        for(Utente u : Registrati.values()) {//cerco nella struttura dati in cui sono presenti tutti i client
            //per verificare che ho trovato l utente corretto controllando l id in allegato al channel
            try {
                u.getReadLock().lock();
                if(u.getLoginChannel().get(ID) != null) return u;
            }
            finally {u.getReadLock().unlock();}
        }
        return null;
    }
}
