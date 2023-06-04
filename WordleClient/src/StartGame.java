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
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

public class StartGame extends JFrame {

    private Socket socket;
    private JTextField UserTEXTLogin;
    private JPasswordField UserTEXTpasslogin;
    private JTextField TExtFieldUserLogout;
    private JTextField TextFieldUserRegistra;
    private JTextField TextFieldPassRegistra;
    private JTextField TextFieldWordSendWord;
    private JLabel NextWordLable;
    private Date DataNextWord = new Date(0);
    private String usernamelogin;
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

                        usernamelogin = UserTEXTLogin.getText();
                        String pass = new String(UserTEXTpasslogin.getText());
                        if(usernamelogin.length() == 0 || pass.length() == 0)return -4;
                        try {
                            DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                            DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                            ou.writeInt((("login:"+ usernamelogin + " " + pass).length())*2);
                            ou.writeChars("login:" + usernamelogin + " " + pass);
                            ou.flush();
                            System.out.print(inn.readInt());

                            switch(inn.readInt()) {
                                case 0 :
                                    //ora qua provo a inviare lo stub al server dopo che mi sono registrato ecc
                                    notifica = new ImplementazioneNotificaClient();
                                    skeleton = (NotificaClient) UnicastRemoteObject.exportObject(notifica, 0);
                                    Registrazione servizio = (Registrazione) LocateRegistry.getRegistry(6500).lookup("Registrazione");;
                                    servizio.sendstub(usernamelogin, skeleton);
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
                                case -4 :
                                    returnvalue = -4;
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
                                    mainPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 100, 20));

                                    JButton Logout = new JButton("Logout");
                                    JButton Gioca = new JButton("Gioca");
                                    JButton SendWord = new JButton("Send");
                                    JButton sendMeStatistics = new JButton("sendMeStatistics");

                                    // Aggiungi gli ascoltatori di azioni ai JButton
                                    Logout.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {

                                            String user = TExtFieldUserLogout.getText();
                                            try {
                                                DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                                                DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                                                ou.writeInt((("logout:"+ user).length())*2);
                                                ou.writeChars("logout:" + user);
                                                ou.flush();
                                                System.out.print(inn.readInt());

                                                switch(inn.readInt()) {
                                                    case 0 :
                                                        UnicastRemoteObject.unexportObject(notifica, true);
                                                        Frame.dispose();
                                                        new StartGame();
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
                                            try {
                                                DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                                                DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                                                ou.writeInt((("playWORDLE:"+ usernamelogin).length())*2);
                                                ou.writeChars("playWORDLE:" + usernamelogin);
                                                ou.flush();
                                                System.out.print(inn.readInt());

                                                switch(inn.readInt()) {
                                                    case 0 :
                                                        //ora qui devo fare in modo da poter visualizzare a schermo data e ora della prossima parola
                                                        // Sto per effettuare modofiche lato server, qui quindi invece di ricever eun long ricevero una
                                                        //stringa, quindi va convertita

                                                        //solo prova
                                                        int len = inn.readInt();
                                                        char [] tmp = new char[len];
                                                        for(int i = 0; i < len; i++) {
                                                            tmp[i] = inn.readChar();
                                                        }


                                                        DataNextWord = new Date(System.currentTimeMillis() + Long.parseLong(new String(tmp)));
                                                        NextWordLable.setText(""+DataNextWord);

                                                        JOptionPane.showMessageDialog(null, "Operazione completata. Adesso è possibile provare a indovinare una porola");
                                                        break;
                                                    case -1 :
                                                        JOptionPane.showMessageDialog(null, "Richiesta di giocare gia effettuata. Inserire la guess word");
                                                        break;
                                                    case -2 :
                                                        JOptionPane.showMessageDialog(null, "Tentativi esauriti per questa sessione. Riprovare a giocare in una nuova sessione");
                                                        break;
                                                }
                                            }
                                            catch (Exception ee) {ee.printStackTrace();}
                                        }
                                    });
                                    SendWord.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {

                                            String word = TextFieldWordSendWord.getText();
                                            if(word.length() == 0) {
                                                JOptionPane.showMessageDialog(null, "Errore. Inserire parola");
                                            }
                                            else {
                                                try {
                                                    DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                                                    DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                                                    ou.writeInt((("sendWord:" + usernamelogin + " " + word).length())*2);
                                                    ou.writeChars("sendWord:" + usernamelogin + " " + word);
                                                    ou.flush();

                                                    int len = inn.readInt();
                                                    int result = inn.readInt();
                                                    System.out.println(result);
                                                    String wordTradotta = null;
                                                    switch(result) {
                                                        case 2 ://caso in cui ho sfruttato l ultimo tentativo e ho perso
                                                                //devo recuperare la parola tradotta

                                                            wordTradotta = ReadData(inn, inn.readInt());
                                                            JOptionPane.showMessageDialog(null, "Tentativi terminati\n Traduzione: " + wordTradotta);


                                                            break;
                                                        case 1 ://caso in cui devo ricevere i suggerimenti

                                                            String sug = ReadData(inn, inn.readInt());//recupero i suggerimenti

                                                            //a questo punto quello che devo fare è visualizzare i suggerimenti in forma grafica
                                                            JPanel suggestionPanle = MakeSuggestionsPanel(sug, word);
                                                            JOptionPane.showMessageDialog(null, suggestionPanle, "Suggerimenti", JOptionPane.PLAIN_MESSAGE);

                                                            break;
                                                        case 0 ://caso in cui la parola è stata indovinata
                                                                // In questo caso lato server dovro inserire la
                                                                // traduzione della parola che qui andra letta

                                                            wordTradotta = ReadData(inn, inn.readInt());
                                                            JOptionPane.showMessageDialog(null, "Vittoria\nTraduzione: " + wordTradotta);

                                                            break;
                                                        case -1:
                                                            JOptionPane.showMessageDialog(null, "Errore. Prima di inviare una parola è necessario chiedere di giocare con il tasto gioca");
                                                            break;
                                                        case -2:
                                                            JOptionPane.showMessageDialog(null, "Tentativi esauriti. Aspettare la prossima parola");
                                                            break;
                                                        case -3:
                                                            JOptionPane.showMessageDialog(null, "Parola gia indovinata. Aspettare la prossima parola");
                                                            break;
                                                        case -4 :
                                                            JOptionPane.showMessageDialog(null, "Parola inesistente all interno del gioco. Il tentativo non verrà considerato");
                                                            break;
                                                    }
                                                }
                                                catch (Exception ee) {ee.printStackTrace();}
                                            }
                                        }
                                    });
                                    sendMeStatistics.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {

                                            //------------------------------------------//
                                            try {
                                                DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                                                DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                                                ou.writeInt((("sendMeStatistics:"+ usernamelogin).length())*2);
                                                ou.writeChars("sendMeStatistics:" + usernamelogin);
                                                ou.flush();
                                                String statistic = gotStatistics(inn);


                                                JOptionPane.showMessageDialog(null, statistic);

                                            }
                                            catch (Exception ee) {ee.printStackTrace();}
                                            //-----------------------------------------------------//
                                        }
                                    });


                                    mainPanel.add(makePanelLogout(Logout));
                                    mainPanel.add(makePanelPlayStart(Gioca));
                                    mainPanel.add(makePanelSend(SendWord));
                                    mainPanel.add(makePanelStatistics(sendMeStatistics));

                                    Frame.add(mainPanel);
                                    Frame.setSize(1000, 500);
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
                                case -4:
                                    JOptionPane.showMessageDialog(null, "Necessario inserire username e password");
                                    break ;
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

                            String user = TextFieldUserRegistra.getText();
                            String pass = new String(TextFieldPassRegistra.getText());
                            Registrazione servizio = (Registrazione) LocateRegistry.getRegistry(6500).lookup("Registrazione");

                            return servizio.registra(user, pass);
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



        mainPanel.add(makePanelLogin(Login));
        mainPanel.add(makePanelRegistrazione(Registra));
        add(mainPanel);
        setSize(new Dimension(1000, 500));
        setVisible(true);

    }
    //metodi utilizzati per creare i panel da inserire nel panel main
    private JPanel makePanelStatistics(JButton sendMeStatistics) {

        JPanel panelStatistics = new JPanel();
        panelStatistics.setLayout(new BoxLayout(panelStatistics, BoxLayout.Y_AXIS));
        panelStatistics.add(new JLabel("User Statistics:"));
        panelStatistics.add(new JLabel(" "));
        panelStatistics.add(new JLabel(" "));
        panelStatistics.add(new JLabel(" "));
        panelStatistics.add(sendMeStatistics);

        return panelStatistics;
    }
    private JPanel makePanelLogin(JButton log) {

        JPanel panelLogin = new JPanel();
        UserTEXTLogin = new JTextField(10);
        UserTEXTpasslogin = new JPasswordField(10);

        panelLogin.setLayout(new FlowLayout());
        panelLogin.add(new JLabel("Login:"));
        panelLogin.add(new JLabel("Username"));
        panelLogin.add(UserTEXTLogin);
        panelLogin.add(new JLabel("Password"));
        panelLogin.add(UserTEXTpasslogin);
        panelLogin.add(log);

        return panelLogin;
    }
    private JPanel makePanelRegistrazione(JButton reg) {

        JPanel panelRegistra = new JPanel();
        TextFieldUserRegistra = new JTextField(10);
        TextFieldPassRegistra = new JPasswordField(10);

        panelRegistra.setLayout(new FlowLayout());
        panelRegistra.add(new JLabel("Registrazione:"));
        panelRegistra.add(new JLabel("Username"));
        panelRegistra.add(TextFieldUserRegistra);
        panelRegistra.add(new JLabel("Password"));
        panelRegistra.add(TextFieldPassRegistra);
        panelRegistra.add(reg);
        return panelRegistra;
    }
    private JPanel makePanelLogout(JButton log) {

        JPanel panelLogout = new JPanel();
        TExtFieldUserLogout = new JTextField(10);

        panelLogout.setLayout(new BoxLayout(panelLogout, BoxLayout.Y_AXIS));
        panelLogout.add(new JLabel("Logout"));
        panelLogout.add(new JLabel(" "));
        panelLogout.add(new JLabel("Nome Utente: " + usernamelogin));//in caso un utente non ricordasse con quale username si è loggato
        panelLogout.add(new JLabel("Username:"));
        panelLogout.add(TExtFieldUserLogout);
        panelLogout.add(log);

        return panelLogout;

    }
    private JPanel makePanelSend(JButton log) {

        JPanel panelSend = new JPanel();
        TextFieldWordSendWord = new JTextField(10);

        panelSend.setLayout(new BoxLayout(panelSend, BoxLayout.Y_AXIS));
        panelSend.add(new JLabel("Send word"));
        panelSend.add(new JLabel(" "));
        panelSend.add(new JLabel("Word:"));
        panelSend.add(TextFieldWordSendWord);
        panelSend.add(log);

        return panelSend;

    }
    private JPanel makePanelPlayStart(JButton play) {

        JPanel panelPlay = new JPanel();
        //NextWordLable =
        panelPlay.setLayout(new BoxLayout(panelPlay, BoxLayout.Y_AXIS));
        panelPlay.add((new JLabel("Data e ora prossima parola prodotta: ")));
        panelPlay.add((new JLabel(" ")));
        panelPlay.add((NextWordLable = new JLabel("Unknown")));
        panelPlay.add(new JLabel("StartSession:"));
        panelPlay.add((new JLabel(" ")));
        panelPlay.add(play);

        return panelPlay;
    }
    public JPanel MakeSuggestionsPanel(String suggestions, String word) {

        JPanel panel = new JPanel(new GridLayout(1, suggestions.length()));

        for(int i = 0; i<10; i++) {//suggestions è lunga 10
            JLabel label = new JLabel(String.valueOf(word.charAt(i)), SwingConstants.CENTER);
            label.setOpaque(true);

            switch (String.valueOf(suggestions.charAt(i))) {
                case "+" :
                    label.setBackground(Color.GREEN);
                    break;
                case "x" :
                    label.setBackground(Color.GRAY);
                    break;
                case "?" :
                    label.setBackground(Color.YELLOW);
                    break;
            }
            panel.add(label);
        }
        return panel;
    }
    private String ReadData(DataInputStream inn, int len) {

        //provo a modificare questo metodo
        int read = 0;
        char [] data = new char[len];
        try {
            while(read < len) {
                data[read] = inn.readChar();
                read++;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return new String(data);
    }
    private String gotStatistics(DataInputStream inn) {

        String answer = new String();
        try {

            int lenMex = inn.readInt();//recupero la lunghezza dell intero messaggio, necessario anche se non usata
            int error = inn.readInt();//recupero l intero che nel mio protocollo indica eventuale errore

            if(error == 0) {//se non ci sono stati errori

                int lenDati = inn.readInt();//recupero la len del mex che contiene le statistiche

                //recupero i dati
                for(int i = 0; i<lenDati; i++) {answer = answer.concat(String.valueOf(inn.readChar()));}
            }
        }
        catch (Exception e) {e.printStackTrace();}

        return answer;

    }
    /*Da capire bene a cosa serve questa cosa
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ClientFrame clientFrame = new ClientFrame();
                clientFrame.setVisible(true);
            }
        });
    }*/

}
