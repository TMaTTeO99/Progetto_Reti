import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerWordle{
    private int PortExport = 6500; // WARNINGGG::: <--per ora uso questa porta per testare
    private Registry RegistroRMI;
    private Registrazione Skeleton;
    private ReentrantReadWriteLock RWlck;//read Write lock in modo da poter avere piu thread che leggono senza bloccarsi dal set dei registrati
    private Lock read;//verra usata per leggere i registrati
    private ExecutorService pool;
    private ArrayList<Utente> Registrati; // Lista che conterrà gli utenti registrati
    private ImlementazioneRegistrazione ObjEsportato;
    public ServerWordle() throws Exception{

        Registrati = new ArrayList<>(); //creo il set degli utenti registrati
        RWlck = new ReentrantReadWriteLock();//inizializzo la read/write lock
        ObjEsportato = new ImlementazioneRegistrazione(Registrati, RWlck);//creo l' oggetto da esportare
        Skeleton = (Registrazione) UnicastRemoteObject.exportObject(ObjEsportato, 0);
        RegistroRMI = LocateRegistry.createRegistry(PortExport);
        RegistroRMI.bind("Registrazione", Skeleton);
        read = RWlck.readLock();
    }

    /**
     * Metodo per effettuare la chiusura del servizio RMI
     */
    public void ShutDownRMI() throws Exception{

        sendNotifica();
        RegistroRMI.unbind("Registrazione");
        UnicastRemoteObject.unexportObject(ObjEsportato, true);
        System.out.println("Chiusura servizsio RMI");//stampa di prova
        PrintRegistrati();
    }
    /**
     * Metodo temporaneo per vedere se la registrazione dei client funziona
     */
    public void PrintRegistrati() {
        for (Utente u : Registrati) {
            System.out.println(u.getUsername() + " " + u.getPassswd());
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
        for (Utente u : Registrati) {
            try {
                u.getStub().SendNotifica(-1);
            }
            catch (Exception e) {e.printStackTrace();}
        }
    }
}
