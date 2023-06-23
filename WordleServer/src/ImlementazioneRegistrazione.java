import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
public class ImlementazioneRegistrazione extends RemoteServer implements Registrazione {

    private ConcurrentHashMap<String, Utente> Registrati;//utenti del gioco
    private LinkedBlockingDeque<DataToSerialize> DaSerializzare;//lista per trasferire le info al thread che deve serializzare i dati
    private Lock LockClassifcaWrite;//lock per per la classifica in scrittura
    private Lock LockClassifcaRead;//lock per per la classifica in lettura
    private ArrayList<UserValoreClassifica> Classifica;
    private HashMap<Integer, String> SecurityKeys;//hashmap di chiavi di sessione

    public ImlementazioneRegistrazione(ConcurrentHashMap<String, Utente> R, LinkedBlockingDeque<DataToSerialize> Lst,
                                       ArrayList<UserValoreClassifica> Clss, Lock LckClss, Lock LckClassRd, HashMap<Integer, String> ScrtKeys) {
        Registrati = R;
        DaSerializzare = Lst;
        Classifica = Clss;
        LockClassifcaWrite = LckClss;
        LockClassifcaRead = LckClassRd;
        SecurityKeys = ScrtKeys;
    }
    public int registra(byte [] username, byte [] passwd, int ID) throws RemoteException {

        int flag_Passwd = Integer.MAX_VALUE;//MAX_VALUE valore di inizializzazione
        String key = SecurityKeys.get(ID);

        //decifro i dati

        String usnameString = SecurityClass.decrypt(username, key);
        String passString = SecurityClass.decrypt(passwd, key);

        if(usnameString == null || passString == null)return 2;//caso in cui decifrazione dei dati fallisce

        //caso in cui il client ha inserito dati non corretti
        if((flag_Passwd = ChckInput(usnameString, passString)) != 0)return flag_Passwd;

        try {
            if(Registrati.putIfAbsent(usnameString, new Utente(usnameString, passString)) == null) {
                DaSerializzare.put(new DataToSerialize(usnameString, 'N'));//il char N indica che sta per arrivare un username

                LockClassifcaWrite.lock();
                    Classifica.add(new UserValoreClassifica(usnameString, 0));//inserisco in classifica l utente appena registrato con score 0
                LockClassifcaWrite.unlock();

                LockClassifcaRead.lock();
                    DaSerializzare.put(new DataToSerialize<>(Classifica, 'C'));
                LockClassifcaRead.unlock();
                return 1;
            }
            return 0;
        }
        catch (Exception e){
            e.printStackTrace();
            return 2;//caso in cui c'è un errore generico
        }
    }
    private int ChckInput(String username, String passwd) {

        int flagnumber = 0, flaguppercase = 0;

        if(username.length() == 0) return -1;
        if(passwd.length() == 0) return -2;
        if(passwd.length() < 5) return -3;
        char [] pass = passwd.toCharArray();
        for(int i = 0; i < pass.length; i++) {
            if((int)pass[i] >= 65 && (int)pass[i] <= 90)flaguppercase = 1;
            if((int)pass[i] >= 48 && (int)pass[i] <= 57)flagnumber = 1;
        }
        if(flaguppercase != 1 || flagnumber != 1)return -4;

        return 0;
    }
    public void RegisryForCallBack(String username, NotificaClient stub) throws RemoteException {
        Registrati.get(username).setStub(stub);
    }
    public void UnRegisryForCallBack(String username, NotificaClient stub)throws RemoteException {
        Registrati.get(username).RemoveSTub();
    }
}
