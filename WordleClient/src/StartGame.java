import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutionException;

public class StartGame extends JFrame {

    private Socket socket;
    private JPanel panelLogin;
    private JPanel panelRegistra;
    private JPanel panelLogout;
    private JPanel panelPlay;
    private NotificaClient skeleton;
    private ImplementazioneNotificaClient notifica;
    public StartGame() {

        // Configurazione del frame principale
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//quando chiudo il frame principale il client termina
        setTitle("Wordle");
        setLocation(new Point(200, 200));

        socket = new Socket();//creo l oggetto socket e mi connetto
        try {socket.connect(new InetSocketAddress("localhost", 6501));}
        catch (Exception e) {e.printStackTrace();}
        //creo il pannel main
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));

        //Pulsanti da aggiungere ai pannel del JFrame
        JButton Login = new JButton("Login");
        JButton Registra = new JButton("Registra");

        //definisco le operazioni che devono effettuare i JButton e le aggiungo
        Login.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Definisco il thread per gestire il login tramite una classe anonima in cui inserisco i metodi
                //doInBackground: lavoro che deve effettuare il thread, è necessario che questo lavoro venga fatto da un thread separato
                //done metodo per far si che quando il thread termini posso in base all risultato della richiesta venga creato
                //un nuovo frame o meno
                SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
                    @Override
                    protected Integer doInBackground() throws Exception {
                        int returnvalue = 1;
                        //----------------------------------------------******************************
                        // Questo pezzo di codice lo devo inserire in un metodo
                        JTextField textField = null;
                        JPasswordField passwordField = null;
                        Component[] components = panelLogin.getComponents();
                        for (Component component : components) {
                            if (component instanceof JTextField) {textField = (JTextField) component;}
                            if(component instanceof JPasswordField){passwordField =  (JPasswordField) component;}

                            if(textField != null && passwordField != null){break;}
                        }
                        String user = textField.getText();
                        String pass = new String(passwordField.getText());

                        try {
                            DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                            DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                            ou.writeInt((("login:"+ user + " " + pass).length())*2);
                            ou.writeChars("login:" + user + " " + pass);
                            ou.flush();
                            System.out.print(inn.readInt());
                            System.out.print(inn.readChar());
                            System.out.print(inn.readChar());
                            System.out.print(inn.readChar());
                            System.out.print(inn.readChar());
                            System.out.print(inn.readChar());
                            System.out.println(inn.readChar());
                            int val = inn.readInt();
                            System.out.println(val);
                            switch(val) {
                                case 0 :
                                    //ora qua provo a inviare lo stub al server dopo che mi sono registrato ecc
                                    notifica = new ImplementazioneNotificaClient();
                                    skeleton = (NotificaClient) UnicastRemoteObject.exportObject(notifica, 0);
                                    Registrazione servizio = (Registrazione) LocateRegistry.getRegistry(6500).lookup("Registrazione");;
                                    servizio.sendstub(user, skeleton);
                                    returnvalue = 1;
                                    break;
                                case -1:
                                    returnvalue = -1;
                                    break;
                                case -2:
                                    returnvalue = -2;
                                    break;
                                case -3:
                                    returnvalue = -3;
                                    break;
                            }
                        }
                        catch (Exception ee) {ee.printStackTrace();}

                        //----------------------------------------------***************************
                        return returnvalue;
                    }
                    @Override
                    protected void done() {
                        try {
                            // Ottieni il risultato della richiesta dal server

                            Integer response = get();  // o false

                            switch (response) {
                                case 1 :
                                    // Chiudo il frame corrente e apro il nuovo frame
                                    dispose();
                                    JFrame Frame = new JFrame("Wordle Game");
                                    Frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                    Frame.setLocation(new Point(200, 200));

                                    JPanel mainPanel = new JPanel();
                                    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));

                                    JButton Logout = new JButton("Logout");
                                    JButton Gioca = new JButton("Gioca");

                                    // Aggiungi gli ascoltatori di azioni ai JButton
                                    Logout.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            JTextField textField = null;
                                            Component[] components = panelLogin.getComponents();
                                            for (Component component : components) {
                                                if (component instanceof JTextField) {
                                                    textField = (JTextField) component;
                                                    break;
                                                }
                                            }
                                            String user = textField.getText();
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
                                                        UnicastRemoteObject.unexportObject(notifica, true);
                                                        Frame.dispose();
                                                        execute();
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
                                    });

                                    Gioca.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            // Avvia il thread per gestire la richiesta 4
                                            // Resto del codice...
                                        }
                                    });

                                    makePanelLogout(Logout);
                                    makePanelPlayStart(Gioca);

                                    mainPanel.add(panelLogout);
                                    mainPanel.add(panelPlay);
                                    Frame.add(mainPanel);
                                    Frame.setSize(200, 200);
                                    Frame.setVisible(true);
                                    break;
                                case -1 :
                                    JOptionPane.showMessageDialog(null, "Errore. Per partecipare al gioco bisogna prima essere iscritti");
                                    break;
                                case -2 :
                                    JOptionPane.showMessageDialog(null, "Errore. Password inserita non corretta");
                                    break;
                                case -3 :
                                    JOptionPane.showMessageDialog(null, "Login gia effettuato");
                                    break;
                            }
                        } catch (Exception e) {
                            // Gestisci eventuali errori di esecuzione della richiesta
                            e.printStackTrace();
                        }
                    }
                };

                // Avvia il worker
                worker.execute();

            }
        });

        Registra.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Avvia il thread per gestire la richiesta 2
                SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
                    @Override
                    protected Integer doInBackground() throws Exception {
                        try {
                            JTextField textField = null;
                            JPasswordField passwordField = null;
                            Component[] components = panelLogin.getComponents();
                            for (Component component : components) {
                                if (component instanceof JTextField) {textField = (JTextField) component;}
                                if(component instanceof JPasswordField){passwordField =  (JPasswordField) component;}

                                if(textField != null && passwordField != null) break;
                            }
                            String user = textField.getText();
                            String pass = new String(passwordField.getText());
                            Registrazione servizio = (Registrazione) LocateRegistry.getRegistry(6500).lookup("Registrazione");
                            if(servizio.registra(user, pass) == 0) return 1;

                            return -1;
                        }
                        catch (Exception exce) {exce.printStackTrace();}
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            // Ottieni il risultato della richiesta dal server
                            int response = get();  // o false
                            switch (response) {
                                case 1 :
                                    JOptionPane.showMessageDialog(null, "Username gia utilizzato");
                                    break;
                                case -1:
                                    JOptionPane.showMessageDialog(null, "Registrazione completata con successo");
                                    break;
                            }

                        } catch (Exception e) {
                            // Gestisci eventuali errori di esecuzione della richiesta
                            e.printStackTrace();
                        }
                    }
                };
                // Avvio il worker
                worker.execute();
            }
        });

        // Aggiungo i pulsanti al frame
        makePanelLogin(Login);
        makePanelRegistrazione(Registra);
        mainPanel.add(panelLogin);
        mainPanel.add(panelRegistra);
        add(mainPanel);
        setSize(new Dimension(1000, 500));
        setVisible(true);

    }
    //metodi utilizzati per creare i panel da inserire nel panel main
    private void makePanelLogin(JButton log) {

        panelLogin = new JPanel();
        JTextField user = new JTextField(10);
        JTextField pass = new JPasswordField(10);

        panelLogin.setLayout(new FlowLayout());
        panelLogin.add(new JLabel("Login:"));
        panelLogin.add(new JLabel("Username"));
        panelLogin.add(user);
        panelLogin.add(new JLabel("Password"));
        panelLogin.add(pass);
        panelLogin.add(log);

    }
    private void makePanelRegistrazione(JButton reg) {

        panelRegistra = new JPanel();
        JTextField user = new JTextField(10);
        JTextField pass = new JPasswordField(10);

        panelRegistra.setLayout(new FlowLayout());
        panelRegistra.add(new JLabel("Registrazione:"));
        panelRegistra.add(new JLabel("Username"));
        panelRegistra.add(user);
        panelRegistra.add(new JLabel("Password"));
        panelRegistra.add(pass);
        panelRegistra.add(reg);

    }
    private void makePanelLogout(JButton log) {

        panelLogout = new JPanel();
        JTextField user = new JTextField(10);

        panelLogout.setLayout(new FlowLayout());
        panelLogout.add(new JLabel("Logout:"));
        panelLogout.add(new JLabel("Username"));
        panelLogout.add(user);
        panelLogout.add(log);


    }
    private void makePanelPlayStart(JButton play) {
        panelPlay = new JPanel();

        panelPlay.setLayout(new FlowLayout());
        panelPlay.add(new JLabel("StartSession:"));
        panelPlay.add(play);

    }
    /*public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ClientFrame clientFrame = new ClientFrame();
                clientFrame.setVisible(true);
            }
        });
    }*/

}
