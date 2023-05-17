import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ImlementazioneRegistrazione extends RemoteServer implements Registrazione {


    //private Lock write;
    private ConcurrentHashMap<String, Utente> Registrati;
    private LinkedBlockingDeque<String> DaSerializzare;
    public ImlementazioneRegistrazione(ConcurrentHashMap<String, Utente> R, LinkedBlockingDeque<String> Lst) {
        Registrati = R;
        DaSerializzare = Lst;
    }

    //NOTA: posso ritornare un valore intero invece che booleano per far
    //      capire al client cosa è successo in caso di registrazione fallita
    //      per motivi diversi da quelli presenti ora, potrebbe essere una feature
    public int registra(String username, String passwd) throws RemoteException {

        //caso in cui il client ha inserito dati non corretti, STOP_THREAD è usato all interno del codice per far terminare
        //il thread che serializza i dati quindi è un username non disponibile per gli utenti
        if(username.length() == 0 || passwd.length() == 0 || username.equals("STOP_THREAD")) return -1;

        try {
            if(Registrati.putIfAbsent(username, new Utente(username, passwd)) == null) {
                DaSerializzare.put(username);
                return 1;
            }
            return 0;
        }
        catch (Exception e){
            e.printStackTrace();
            return 1;
        }
    }
    public void sendstub(String username, NotificaClient stub) throws RemoteException {
        Registrati.get(username).setStub(stub);
    }
}
