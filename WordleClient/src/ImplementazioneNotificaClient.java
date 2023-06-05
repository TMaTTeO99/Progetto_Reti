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
        /*String notifica = new String("<html>");
        for(int i = 0; i< podio.size(); i++) {
            notifica = notifica.concat("<br>" + "USER: " + podio.get(i).getUsername() + " SCORE: " + podio.get(i).getScore());
        }
        notifica = notifica.concat("</html>");
        System.out.println(notifica);
        */
        classifica.setText("Nuova notifica");
    }
    public ArrayList<UserValoreClassifica> GetClassififca() {return Podio;}
}
