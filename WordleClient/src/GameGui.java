import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.rmi.server.UnicastRemoteObject;

public class GameGui {

    //per ora faccio solo la parte di logout
    private Socket socket;
    private ImplementazioneNotificaClient notifica;
    private JTextField userLogout;//TextField per il tasto di logout
    private JFrame frame; //Frame principale della sessione di gioco
    private String username;
    public GameGui(Socket sck, ImplementazioneNotificaClient ntf, String usr) {

        socket = sck;
        notifica = ntf;
        username = usr;

        //costruzione dal frame
        frame = new JFrame();
        frame = new JFrame("Wordle Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(new Point(200, 200));

        //costruziohe dei panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        JPanel panelLogout = getPanelLogout();
        JPanel panelPlayStart = getPanelPlayStart();
        LogoutMethod(panelLogout);
        PlayGameMethod(panelPlayStart);

        mainPanel.add(panelLogout);
        mainPanel.add(panelPlayStart);




        frame.getContentPane().add(mainPanel);
        frame.setSize(new Dimension(1000, 500));
        frame.setVisible(true);

    }

    //metodi per il lavoro dei button presenti nei panel
    private void PlayGameMethod(JPanel panelPlayStart) {

        JButton play = new JButton("Gioca");

        ActionListener listenerPlay = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                byte [] tmp = new byte[100];
                ByteArrayInputStream BuffIn = new ByteArrayInputStream(tmp);
                try {
                    DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    ou.writeInt((("playWORDLE:"+ username).length())*2);
                    ou.writeChars("playWORDLE:" + username);
                    ou.flush();
                    System.out.print(inn.readInt());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.print(inn.readChar());
                    System.out.println(inn.readChar());

                    switch(inn.readInt()) {
                        case 0 :
                            JOptionPane.showMessageDialog(null, "Operazione completata. Adesso è possibile provare a indovinare una porola");
                            break;
                        case -1 :
                            JOptionPane.showMessageDialog(null, "Richiesta di giocare gia effettuata. Inserire la guess wordle");
                            break;
                        case -2 :
                            JOptionPane.showMessageDialog(null, "Tentativi esauriti per questa sessione. Riprovare a giocare in una nuova sessione");
                            break;
                    }
                }
                catch (Exception ee) {ee.printStackTrace();}
            }
        };
        play.addActionListener(listenerPlay);
        panelPlayStart.add(play);

    }
    private void LogoutMethod(JPanel logoutPanel) {

        JButton logout = new JButton("Esci");

        //WORNINGGGGG:::::anche in questo caso uso classe astratta, molto utili, da capire fino in fondo
        ActionListener listenerLogout = new ActionListener() {
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
                            UnicastRemoteObject.unexportObject(notifica, true);
                            StartGui gg = new StartGui();//avvio il frame di gioco
                            timer.start();//chiudo il frame iniziale
                            break;
                        case -1:
                            JOptionPane.showMessageDialog(null, "Errore. Username inserito non corretto");
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
        logout.addActionListener(listenerLogout);
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
    private JPanel getPanelPlayStart() {
        JPanel panel = new JPanel();

        panel.setLayout(new FlowLayout());
        panel.add(new JLabel("StartSession:"));

        return panel;
    }



}
