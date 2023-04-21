import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ImlementazioneRegistrazione extends RemoteServer implements Registrazione {


    private ReentrantReadWriteLock Lck;
    private Lock write;
    private ArrayList<Utente> Registrati;
    public ImlementazioneRegistrazione(ArrayList<Utente> R, ReentrantReadWriteLock Lck) {
        Registrati = R;
        write = Lck.writeLock();
    }

    //NOTA: posso ritornare un valore intero invece che booleano per far
    //      capire al client cosa è successo in caso di registrazione fallita
    //      per motivi diversi da quelli presenti ora, potrebbe essere una feature
    public boolean registra(String username, String passwd, NotificaClient stub) throws RemoteException {

        Utente u = new Utente(username, passwd, stub);
        try {
            write.lock();
            if(Registrati.contains(u)){
                System.out.println("Registrazione fallita");//stampa di check, da eliminare
                return false;
            }
            Registrati.add(u);
            System.out.println("Utente Registrato");//stampa di check, da eliminare
            return true;
        }
        finally {
            write.unlock();
        }
    }
}
