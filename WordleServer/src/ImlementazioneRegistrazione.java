import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ImlementazioneRegistrazione extends RemoteServer implements Registrazione {


    private Lock write;
    private HashMap<String, Utente> Registrati;
    private LinkedBlockingDeque<String> DaSerializzare;
    public ImlementazioneRegistrazione(HashMap<String, Utente> R, ReentrantReadWriteLock Lck, LinkedBlockingDeque<String> Lst) {
        Registrati = R;
        write = Lck.writeLock();
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
            write.lock();
            Utente u = new Utente(username, passwd);
            if(Registrati.containsKey(username)){return 0;}//caso in cui l'utente è gia registrato
            Registrati.put(username, u);
            DaSerializzare.put(username);
            return 1;
        }
        catch (Exception e){
            e.printStackTrace();
            return 1;
        }
        finally {
            write.unlock();
        }
    }
    public void sendstub(String username, NotificaClient stub) throws RemoteException {

        try {
            write.lock();
            Registrati.get(username).setStub(stub);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            write.unlock();
        }
    }
}
