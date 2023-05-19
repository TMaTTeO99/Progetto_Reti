import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class RegistrGui implements ActionListener {

    private JTextField uname;
    private JTextField passwd;

    public RegistrGui(JTextField u, JTextField p) {
        uname = u;
        passwd = p;
    }

    public void actionPerformed(ActionEvent e) {

        //in questo metodo dovro aggiungere l implementazione gia fatta nel main per effettuare la registrazione
        try {
            Registrazione servizio = (Registrazione) LocateRegistry.getRegistry(6500).lookup("Registrazione");
            if(servizio.registra(uname.getText(), new String(passwd.getText())) == 0) JOptionPane.showMessageDialog(null, "Username gia utilizzato");
            else JOptionPane.showMessageDialog(null, "Registrazione completata con successo");
        }
        catch (Exception exce) {exce.printStackTrace();}

    }
}
