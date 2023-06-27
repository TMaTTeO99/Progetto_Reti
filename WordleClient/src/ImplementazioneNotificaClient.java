import javax.swing.*;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;

public class ImplementazioneNotificaClient extends RemoteServer implements NotificaClient {

    private JLabel classifica;//lable che viene modificata quando si riceve una notifica
    private ArrayList<UserValoreClassifica> Podio;//arraylist che conterrà le prime 3 posizioni della classifica
    public ImplementazioneNotificaClient(JLabel cls) {classifica = cls;}
    public void SendNotifica(ArrayList<UserValoreClassifica> podio) throws RemoteException {

        Podio = podio;
        classifica.setText("Nuova notifica");
    }
    public ArrayList<UserValoreClassifica> GetClassififca() {return Podio;}
}
