import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

//Interfaccia usata dal client per esportare l'oggetto che sarà inviato nella fase di registraziopne al server
//per far si che il server possa notificare il client quando necessario, per ora uso un intero come notifica per distinguere
//il tipo della notifica ricevuta
public interface NotificaClient extends Remote {
    public void SendNotifica(ArrayList<UserValoreClassifica> podio) throws RemoteException;

}
