import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;

public class StartLoginRegistrazione extends JFrame {

    private Socket socket;
    private JTextField UserTEXTLogin;
    private JPasswordField UserTEXTpasslogin;
    private JTextField TextFieldUserRegistra;
    private JTextField TextFieldPassRegistra;
    private JLabel Classifica = new JLabel("Nessuna Notifica");
    private String usernamelogin;
    private Registrazione servizio = null;
    private String IP_server;
    private String IP_Multicast;
    private int Port_listening;
    private int Port_Multicast;
    private int PortRMI;
    private ArrayList<Suggerimenti> SuggerimentiQueue;
    public StartLoginRegistrazione(String IP_srv, int Port_list, String IP_Mltc, int Port_Mltc, int PrtRMI, ArrayList<Suggerimenti> SuggQueue) throws Exception {

        //recupero all inetrno delle var di istanza le info che servono al client
        IP_server = IP_srv;
        Port_listening = Port_list;
        IP_Multicast = IP_Mltc;
        Port_Multicast = Port_Mltc;
        PortRMI = PrtRMI;
        SuggerimentiQueue = SuggQueue;

        //recupero il servizio di registrazione
        servizio = (Registrazione) LocateRegistry.getRegistry(PortRMI).lookup("Registrazione");

        //configurazione del frame principale
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//quando chiudo il frame principale il client termina
        setTitle("Wordle");
        setLocation(new Point(200, 200));
        setLayout(new GridLayout());

        socket = new Socket();//creo l oggetto socket e mi connetto al server appena avvio
        try {socket.connect(new InetSocketAddress(IP_server, Port_listening));}
        catch (Exception e) {e.printStackTrace();}

        //creo il pannel main
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));

        //-------------------------------------------------------------------------------------------//

            /**
             * Sezione in cui provero a comunicare con il server per scambiare la chiave di sessione
             * Sara una chiave casuale che verra usata per cifrare le passwd e le parole che i client
             * inviano, non dovrebbe esserci bisogno di cifrare tutto anche se comunque posso provare a cifrare
             * tutto, devo decidere l algo di scambio di chiave e l algo di cifratura.
             */

        //-------------------------------------------------------------------------------------------//


        //Pulsanti da aggiungere ai pannel del JFrame
        JButton Login = new JButton("Login");
        JButton Registra = new JButton("Registra");

        //definisco le operazioni che devono effettuare i JButton e le aggiungo
        Login.addActionListener(e -> {StartLogin();});
        Registra.addActionListener(e -> {StartRegistra();});

        // Aggiungo i pulsanti al frame
        mainPanel.add(makePanelLogin(Login));
        mainPanel.add(makePanelRegistrazione(Registra));
        add(mainPanel);
        add(makePanelRules());
        setSize(new Dimension(1000, 500));
        setVisible(true);

    }
    //metodi utilizzati per creare i panel da inserire nei mainpanel
    private JPanel makePanelRules() {


        JPanel panelShowRules = new JPanel();
        JLabel tmp = null;

        panelShowRules.setPreferredSize(new Dimension(20, 20));
        panelShowRules.setBorder(BorderFactory.createTitledBorder("OVERVIEW GAME: "));
        panelShowRules.setLayout(new BoxLayout(panelShowRules, BoxLayout.Y_AXIS));
        panelShowRules.add(tmp = new JLabel("<html>Il gioco wordle consiste nel cercare di indovinare una parola inglese formata da 10 lettere.<br>" +
                "Periodicamente viene proposta una nuova parola<br> che l'utente dovrà provare a indovinare in un massimo di 12 tentativi.<br>" +
                "Se la parola scelta dall utente non è presente nel vocabolario del gioco il tentativo non verrà considerato.<br>" +
                "Dopo aver effettuato il login sarà presente il pulsante (HELP) che guiderà l utente per l utilizzo delle altre funzionalità " +
                "proposte dal gioco <br>" +
                "<br>Le regole sono le seguenti: <br>" +
                "<br>1) Prima di effettuare il login è necessario iscriversi<br>" +
                "2) Prima di poter tentare di indovinare una parola è necessario chiedere di giocare<br>" +
                "3) Se un utente chiede di giocare ed effettua il logout prima di aver indovinato la parola la partita è considerata persa<br>" +
                "4) Se un utente esaurisce i 12 tentativi senza indovinare la parola la partita è considerata persa <br>" +
                "5) Se un utente comincia una partita e dubito dopo viene aggiornata la parola del gioco la partita è considerata persa</html>"
        ));
        panelShowRules.setBackground(new Color(192, 166, 209));
        return panelShowRules;

    }
    private JPanel makePanelLogin(JButton log) {

        JPanel panelLogin = new JPanel();
        panelLogin.setPreferredSize(new Dimension(20, 20));
        panelLogin.setBorder(BorderFactory.createTitledBorder("Login: "));
        UserTEXTLogin = new JTextField(10);
        UserTEXTpasslogin = new JPasswordField(10);

        panelLogin.setLayout(new FlowLayout());
        panelLogin.add(new JLabel("Username"));
        panelLogin.add(UserTEXTLogin);
        panelLogin.add(new JLabel("Password"));
        panelLogin.add(UserTEXTpasslogin);
        panelLogin.add(log);
        panelLogin.setBackground(new Color(192, 166, 209));

        return panelLogin;
    }
    private JPanel makePanelRegistrazione(JButton reg) {

        JPanel panelRegistra = new JPanel();
        panelRegistra.setPreferredSize(new Dimension(20, 20));
        panelRegistra.setBorder(BorderFactory.createTitledBorder("Registrazione:"));
        TextFieldUserRegistra = new JTextField(10);
        TextFieldPassRegistra = new JPasswordField(10);

        panelRegistra.setLayout(new FlowLayout());
        panelRegistra.add(new JLabel("Username"));
        panelRegistra.add(TextFieldUserRegistra);
        panelRegistra.add(new JLabel("Password"));
        panelRegistra.add(TextFieldPassRegistra);
        panelRegistra.add(reg);
        panelRegistra.setBackground(new Color(192, 166, 209));

        return panelRegistra;
    }


    //sezione che contiene i metodi utilizzati all interno delle espressioni lambda
    private void StartLogin() {

        SwingWorker<ReturnPackage, Void> worker = new SwingWorker<ReturnPackage, Void>() {

            @Override
            protected ReturnPackage doInBackground() throws Exception {

                int returnvalue = Integer.MAX_VALUE;//MAX_VALUE usato solo come valore di inizializzazione

                usernamelogin = UserTEXTLogin.getText();
                String pass = new String(UserTEXTpasslogin.getPassword());//qui prima era getText, l ho modificato

                if(usernamelogin.length() == 0 || pass.length() == 0)return new ReturnPackage( -4);
                try {
                    DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    ou.writeInt((("login:"+ usernamelogin + " " + pass).length())*2);
                    ou.writeChars("login:" + usernamelogin + " " + pass);
                    ou.flush();
                    inn.readInt(); //scarto la len del messaggio
                    returnvalue = inn.readInt();//recupero il valore di ritorno dal server

                }
                catch (Exception ee) {ee.printStackTrace();}

                return new ReturnPackage(returnvalue);
            }
            @Override
            protected void done() {
                try {

                    Integer response = get().getReturnValue();  // Recupero il valore di ritorno del metodo doInBackground

                    switch (response) {

                        case 0 ://caso in cui l utente ha effettuato il login con successo.

                            dispose();// Chiudo il frame corrente
                            new StartGame(IP_server, Port_listening, IP_Multicast, Port_Multicast, PortRMI, socket, usernamelogin, servizio, SuggerimentiQueue);
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
                        case -4:
                            JOptionPane.showMessageDialog(null, "Necessario inserire username e password");
                            break ;
                    }
                }
                catch (Exception e) {
                    // Gestisci eventuali errori di esecuzione della richiesta
                    System.out.println("CATCH ESTERNO");
                    e.printStackTrace();
                }
            }
        };
        //avvio il worker
        worker.execute();
    }
    private void StartRegistra() {
        // Avvia il thread per gestire la richiesta 2
        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                try {

                    String user = TextFieldUserRegistra.getText();
                    String pass = new String(TextFieldPassRegistra.getText());
                    //Registrazione servizio = (Registrazione) LocateRegistry.getRegistry(6500).lookup("Registrazione");

                    return servizio.registra(user, pass);
                }
                catch (Exception exce) {exce.printStackTrace();}
                return null;
            }

            @Override
            protected void done() {
                try {

                    int response = get();
                    switch (response) {
                        case 1 :
                            JOptionPane.showMessageDialog(null, "Registrazione completata");
                            break;
                        case -1:
                            JOptionPane.showMessageDialog(null, "Errore nei dati inseriti");
                            break;
                        case 0:
                            JOptionPane.showMessageDialog(null, "Username gia utilizzato");
                            break;
                        case 2:
                            JOptionPane.showMessageDialog(null, "Errore");
                            break;
                    }

                }
                catch (Exception e) {e.printStackTrace();}
            }
        };
        // Avvio il worker
        worker.execute();
    }

    /*Da capire bene a cosa serve questa cosa
    A regolòa questo pezzo di codice dovrei inserirlo delntro lo start client main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ClientFrame clientFrame = new ClientFrame();
                clientFrame.setVisible(true);
            }
        });
    }*/

}