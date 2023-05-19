import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;

public class GameGui {

    //per ora faccio solo la parte di logout
    private Socket socket;
    private ImplementazioneNotificaClient notifica;
    public GameGui(Socket sck, ImplementazioneNotificaClient notifica) {
        socket = sck;
        this.notifica = notifica;

        JFrame frame = new JFrame();
        frame = new JFrame("Wordle");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(new Point(200, 200));
        JPanel panel = getPanelLogouty();
        frame.getContentPane().add(panel);
        frame.setSize(new Dimension(1000, 500));
        frame.setVisible(true);

    }

    private JPanel getPanelLogouty() {

        JPanel panel = new JPanel();
        JButton logout = new JButton("Esci");
        JTextField user = new JTextField(10);

        panel.setLayout(new FlowLayout());
        panel.add(new JLabel("Username"));
        panel.add(user);
        //anche in questo caso uso classe astratta, molto utili, da capire fino in fondo
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Logoutprova");
            }
        };
        logout.addActionListener(listener);
        return panel;

    }
//UnicastRemoteObject.unexportObject(notifica, true);

}
