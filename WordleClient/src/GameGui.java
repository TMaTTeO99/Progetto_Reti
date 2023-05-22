import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class GameGui {

    //per ora faccio solo la parte di logout
    private Socket socket;
    private ImplementazioneNotificaClient notifica;
    private JTextField userLogout;//TextField per il tasto di logout
    private JFrame frame; //Frame principale della sessione di gioco
    public GameGui(Socket sck, ImplementazioneNotificaClient notifica) {

        socket = sck;
        this.notifica = notifica;

        //costruzione dal frame
        frame = new JFrame();
        frame = new JFrame("Wordle Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(new Point(200, 200));

        //costruziohe dei panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        JPanel panelLogout = getPanelLogout();
        LogoutMethod(panelLogout);


        mainPanel.add(panelLogout);





        frame.getContentPane().add(mainPanel);
        frame.setSize(new Dimension(1000, 500));
        frame.setVisible(true);

    }

    //metodi per il lavoro dei button presenti nei panel

    private void LogoutMethod(JPanel logoutPanel) {

        JButton logout = new JButton("Esci");

        //anche in questo caso uso classe astratta, molto utili, da capire fino in fondo
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String user = new String(userLogout.getText());
                byte [] tmp = new byte[100];
                ByteArrayInputStream BuffIn = new ByteArrayInputStream(tmp);
                try {
                    DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    ou.writeInt((("logout:"+ user).length())*2);
                    ou.writeChars("logout:" + user);
                    ou.flush();
                    System.out.print(inn.readInt());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.println(inn.readChar());

                    switch(inn.readInt()) {
                        case 0 :
                            Timer timer = new Timer(0, new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    frame.dispose(); // Chiude il frame
                                }
                            });
                            StartGui gg = new StartGui();//avvio il frame di gioco
                            timer.start();//chiudo il frame iniziale
                            break;
                        case -1:
                            JOptionPane.showMessageDialog(null, "Errore. Per effettuare il logout bisogna prima essere iscritti");
                            break;
                        case -2:
                            JOptionPane.showMessageDialog(null, "Errore. Per effettuare il logout bisogna prima aver effettuato il login");
                            break;
                        case -3:
                            JOptionPane.showMessageDialog(null, "Errore. Username inserito non corretto");
                            break;
                    }
                }
                catch (Exception ee) {ee.printStackTrace();}
            }
        };
        logout.addActionListener(listener);
        logoutPanel.add(logout);

    }


    //metodi per costruire i ponel
    private JPanel getPanelLogout() {

        JPanel panel = new JPanel();
        userLogout = new JTextField(10);

        panel.setLayout(new FlowLayout());
        panel.add(new JLabel("Logout:"));
        panel.add(new JLabel("Username"));
        panel.add(userLogout);
        return panel;

    }
//UnicastRemoteObject.unexportObject(notifica, true); esporto l'oggetto notifica alla fine del metodo in questo modo il client non riceverà piu notifiche

}
