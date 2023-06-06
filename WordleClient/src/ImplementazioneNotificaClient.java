import javax.swing.*;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;

public class ImplementazioneNotificaClient extends RemoteServer implements NotificaClient {

    private JLabel classifica;
    private ArrayList<UserValoreClassifica> Podio;
    public ImplementazioneNotificaClient(JLabel cls) {classifica = cls;}
    public void SendNotifica(ArrayList<UserValoreClassifica> podio) throws RemoteException {

        Podio = podio;
        classifica.setText("Nuova notifica");
    }
    public ArrayList<UserValoreClassifica> GetClassififca() {return Podio;}
}
