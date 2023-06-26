import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.security.MessageDigest;
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

        try {

            String key = SecurityKeys.get(ID);//recupero la chiave per la decifrazione dei dati

            //decifro i dati
            String usnameString = SecurityClass.decrypt(username, key);
            String passString = SecurityClass.decrypt(passwd, key);

            if(usnameString == null || passString == null)return 2;//caso in cui decifrazione dei dati fallisce

            //caso in cui il client ha inserito dati non corretti
            if((flag_Passwd = ChckInput(usnameString, passString)) != 0)return flag_Passwd;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");//recupo la funzione HASH
            byte[] encodedhash = digest.digest(passString.getBytes(StandardCharsets.UTF_8));


            //se il client non è presente fra i registrati, se non c'è lo inserisco
            //come password uso l immagine hash della password all interno del ogetto utente

            if(Registrati.putIfAbsent(usnameString, new Utente(usnameString, bytesToHex(encodedhash))) == null) {
                //comunico al thread che serializza che è presente un nuovo utente e che quindi potrebbe dover serializzare
                DaSerializzare.put(new DataToSerialize(usnameString, 'N'));//il char N indica che sta per arrivare un username

                LockClassifcaWrite.lock();
                    Classifica.add(new UserValoreClassifica(usnameString, 0));//inserisco in classifica l utente appena registrato con score 0
                LockClassifcaWrite.unlock();

                //comunico al thread che serializza di serializzare la classifica perche un nuovo utente entra in classifica
                //uso le variabili di lock perche qualche altro thread potrebbe accedere alla classifica
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
    //metodo che controlla la correttezza dei dati in ingresso
    private int ChckInput(String username, String passwd) {

        int flagnumber = 0, flaguppercase = 0;

        //controllo la len dei dati
        if(username.length() == 0) return -1;
        if(passwd.length() == 0) return -2;
        if(passwd.length() < 5) return -3;

        //controllo che all interno della password sia presente almeno un numero e una lettera maiuscola
        char [] pass = passwd.toCharArray();
        for(int i = 0; i < pass.length; i++) {
            if((int)pass[i] >= 65 && (int)pass[i] <= 90)flaguppercase = 1;
            if((int)pass[i] >= 48 && (int)pass[i] <= 57)flagnumber = 1;
        }
        if(flaguppercase != 1 || flagnumber != 1)return -4;

        return 0;
    }
    private String bytesToHex(byte[] hash) {

        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);//recupero  il byte senza il segno
            if(hex.length() == 1) {//se hex ha lunghezza 1 aggingo uno 0 per avere una rappresentazione esadecimale con 2 cifre
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    public void RegisryForCallBack(String username, NotificaClient stub) throws RemoteException {

        Utente u = Registrati.get(username);
        try {
            u.getWriteLock().lock();
            u.setStub(stub);
        }
        finally {u.getWriteLock().unlock();}

    }
    public void UnRegisryForCallBack(String username, NotificaClient stub)throws RemoteException {

        Utente u = Registrati.get(username);
        try {
            u.getWriteLock().lock();
            u.RemoveSTub();
        }
        finally {u.getWriteLock().unlock();}
    }
}
