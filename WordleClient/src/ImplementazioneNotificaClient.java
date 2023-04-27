import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;

public class ImplementazioneNotificaClient extends RemoteServer implements NotificaClient {
    public void SendNotifica(int notifica) throws RemoteException {
        System.out.println("Tipo di notifica ricevuta: " + notifica);
        if(notifica == -1)System.out.println("Chiusura del servizio di registrazione");//stampa per indicare cosa succede
    }
}
