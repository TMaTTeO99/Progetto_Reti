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
    private int PortExport = 6500; // WARNINGGG::: <--per ora uso questa porta per testare
    private Registry RegistroRMI;
    private Registrazione Skeleton;
    private ReentrantReadWriteLock RWlck;//read Write lock in modo da poter avere piu thread che leggono senza bloccarsi dal hashmap dei registrati
    private Lock read;//verra usata per leggere gli utenti registrati e fare i vari controlli
    private ExecutorService pool;
    private HashMap<String, Utente> Registrati; // Lista che conterrà gli utenti registrati
    private ImlementazioneRegistrazione ObjEsportato;//variabile per gestire la condition variable per il thread che scrive su json
    private LinkedBlockingDeque<String> DaSerializzare;//lista di supporto usata per capire quali utenti devono essere serializzati
                                             //ad ogni iscrizione
    public ServerWordle(int Nthread, long TimeStempWord) throws Exception{

        DaSerializzare = new LinkedBlockingDeque<>();
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Nthread);//pool di thread per eseguire i diversi task
        Registrati = new HashMap<>(); //creo il set degli utenti registrati
        RWlck = new ReentrantReadWriteLock();//inizializzo la read/write lock
        read = RWlck.readLock();
        ObjEsportato = new ImlementazioneRegistrazione(Registrati, RWlck, DaSerializzare);//creo l' oggetto da esportare
        Skeleton = (Registrazione) UnicastRemoteObject.exportObject(ObjEsportato, 0);
        RegistroRMI = LocateRegistry.createRegistry(PortExport);
        RegistroRMI.bind("Registrazione", Skeleton);
        pool.execute(new MakeJson(Registrati, DaSerializzare));
    }

    /**
     * Metodo per effettuare la chiusura del servizio RMI,
     * quando tale servizio viene chiuso il server viene spento
     */
    public void ShutDownRMI() throws Exception{

        sendNotifica();
        RegistroRMI.unbind("Registrazione");
        UnicastRemoteObject.unexportObject(ObjEsportato, true);
        System.out.println("Chiusura servizsio RMI");//stampa di prova
        pool.shutdown();
        while(!pool.isTerminated()){
            pool.awaitTermination(60L, TimeUnit.MILLISECONDS);
        }
        PrintRegistrati();
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
}
