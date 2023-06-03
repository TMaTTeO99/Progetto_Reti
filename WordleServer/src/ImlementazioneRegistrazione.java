import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ImlementazioneRegistrazione extends RemoteServer implements Registrazione {

    private ConcurrentHashMap<String, Utente> Registrati;//utenti del gioco
    private LinkedBlockingDeque<DataToSerialize> DaSerializzare;//lista per trasferire le info al thread che deve serializzare i dati
    private Lock LockClassifca;//lock per implementare mutua esclusione classifica
    private ArrayList<UserValoreClassifica> Classifica;

    public ImlementazioneRegistrazione(ConcurrentHashMap<String, Utente> R, LinkedBlockingDeque<DataToSerialize> Lst,
                                       ArrayList<UserValoreClassifica> Clss, Lock LckClss) {
        Registrati = R;
        DaSerializzare = Lst;
        Classifica = Clss;
        LockClassifca = LckClss;

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
                DaSerializzare.put(new DataToSerialize(username, 'N'));//il char N indica che sta per arrivare un username

                LockClassifca.lock();
                    Classifica.add(new UserValoreClassifica(username, 0));//inserisco in classifica l utente appena registrato con score 0
                LockClassifca.unlock();
                return 1;
            }
            return 0;
        }
        catch (Exception e){
            e.printStackTrace();
            return 2;
        }
    }
    public void sendstub(String username, NotificaClient stub) throws RemoteException {
        Registrati.get(username).setStub(stub);
    }
}
