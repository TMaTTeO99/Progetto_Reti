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
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StartGame extends JFrame {

    private Socket socket;
    private JTextField UserTEXTLogin;
    private JPasswordField UserTEXTpasslogin;
    private JTextField TExtFieldUserLogout;
    private JTextField TextFieldUserRegistra;
    private JTextField TextFieldPassRegistra;
    private JTextField TextFieldWordSendWord;
    private JLabel NextWordLable;
    private JLabel Classifica = new JLabel("Nessuna Notifica");
    private Date DataNextWord = new Date(0);
    private String usernamelogin;
    private NotificaClient skeleton;
    private ImplementazioneNotificaClient notifica;

    private ArrayList<Suggerimenti> SuggerimentiQueueTemp = new ArrayList<>();

    private ReentrantLock locksuggerimenti = new ReentrantLock();//lock usata per implementare mutua esclusione sulla coda dei suggerimenti


    //private LinkedBlockingDeque<Suggerimenti> SuggerimentiQueue = new LinkedBlockingDeque<>();
    Registrazione servizio = null;
    public StartGame() throws Exception {

        //recupero il servizio di registrazione
        servizio = (Registrazione) LocateRegistry.getRegistry(6500).lookup("Registrazione");;

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
                        String pass = new String(UserTEXTpasslogin.getPassword());//qui prima era getText, l ho modificato
                        if(usernamelogin.length() == 0 || pass.length() == 0)return -4;
                        try {
                            DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                            DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                            ou.writeInt((("login:"+ usernamelogin + " " + pass).length())*2);
                            ou.writeChars("login:" + usernamelogin + " " + pass);
                            ou.flush();
                            System.out.print(inn.readInt() + "1");

                            switch(inn.readInt()) {
                                case 0 :
                                    //ora qua provo a inviare lo stub al server dopo che mi sono registrato ecc
                                    notifica = new ImplementazioneNotificaClient(Classifica);
                                    skeleton = (NotificaClient) UnicastRemoteObject.exportObject(notifica, 0);
                                    servizio.RegisryForCallBack(usernamelogin, skeleton);
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

                                    JButton ShowMeSharing = new JButton("Visualizza Condivisioni");
                                    JButton Visualizza = new JButton("Visualizza");
                                    JButton Logout = new JButton("Logout");
                                    JButton Gioca = new JButton("Gioca");
                                    JButton SendWord = new JButton("Send");
                                    JButton sendMeStatistics = new JButton("sendMeStatistics");
                                    JButton ShowMeRancking = new JButton("showMeRanking");
                                    JButton Share = new JButton("Share");
                                    JButton TimeNextWord = new JButton("TimeNextWord");

                                    //a questo punto quello che faccio è lanciare un thread che sta i ascolto
                                    //dei dati che vengono inviati dal server sul gruppo multicast
                                    //porta e ip dovranno essere dati presi dalfile di config
                                    Thread multiCast = new Thread(new CaptureUDPmessages("239.0.0.1", 5240, SuggerimentiQueueTemp, locksuggerimenti));
                                    multiCast.start();

                                    //Aggiungo gli ascoltatori di azioni ai JButton
                                    TimeNextWord.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {

                                            try {
                                                DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                                                DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                                                ou.writeInt((("TimeNextWord:" + usernamelogin).length())*2);
                                                ou.writeChars("TimeNextWord:" + usernamelogin);
                                                ou.flush();
                                                System.out.print(inn.readInt()+ "3");
                                                switch (inn.readInt()) {

                                                    case 0 :
                                                        //ora qui devo fare in modo da poter visualizzare a schermo data e ora della prossima parola
                                                        // Sto per effettuare modofiche lato server, qui quindi invece di ricever eun long ricevero una
                                                        //stringa, quindi va convertita

                                                        //solo prova anche qua posso usare readData
                                                        int len = inn.readInt();
                                                        char [] tmp = new char[len];
                                                        for(int i = 0; i < len; i++) {
                                                            tmp[i] = inn.readChar();
                                                        }

                                                        DataNextWord = new Date(System.currentTimeMillis() + Long.parseLong(new String(tmp)));
                                                        NextWordLable.setText(""+DataNextWord);

                                                        break;
                                                    default :
                                                        JOptionPane.showMessageDialog(null, "ERRORE");
                                                        break;

                                                }

                                            }
                                            catch (Exception ee) {ee.printStackTrace();}
                                        }
                                    });
                                    ShowMeSharing.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {

                                            //cerco di estrarre dalla coda finche la cosa è piena
                                            Suggerimenti datiCondivisi = null;
                                            JFrame shareFrame = new JFrame("SUGGERIMENTI CONDIVISI");
                                            shareFrame.setLayout(new BoxLayout(shareFrame.getContentPane(), BoxLayout.Y_AXIS));
                                            shareFrame.setLocation(new Point(300, 300));

                                            try {
                                                locksuggerimenti.lock();
                                                if(SuggerimentiQueueTemp.size() != 0) {

                                                    int i = 0;

                                                    System.out.println("PRIMA DEL PANNEL e dopo AWAIT");
                                                    System.out.println(SuggerimentiQueueTemp.size());
                                                    while(i < SuggerimentiQueueTemp.size()) {

                                                        //qui devo trovare il modo di visualizzare i suggerimenti in un unico panel
                                                        shareFrame.add(MakeAllSuggestionsPanel(SuggerimentiQueueTemp.get(i)));
                                                        i++;
                                                    }

                                                    shareFrame.setSize(200, 200);
                                                    shareFrame.setVisible(true);
                                                }
                                                else {
                                                    JOptionPane.showMessageDialog(null, "Nessuna Notifica");
                                                }
                                            }
                                            catch (Exception ex) {ex.printStackTrace();}
                                            finally {locksuggerimenti.unlock();}
                                        }
                                    });
                                    Share.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {

                                            try {
                                                DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                                                DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                                                ou.writeInt((("share:"+ usernamelogin).length())*2);
                                                ou.writeChars("share:" + usernamelogin);
                                                ou.flush();
                                                System.out.print(inn.readInt()+ "3");

                                                switch(inn.readInt()) {
                                                    case 0 :
                                                        JOptionPane.showMessageDialog(null, "Operazione Completata");
                                                        break;
                                                    case -1 :
                                                        JOptionPane.showMessageDialog(null, "Errore. Giocare al gioco prima di condividere i risultati");
                                                        break;
                                                    case -2:
                                                        JOptionPane.showMessageDialog(null, "Errore. Prima di poter condividere i tentativi bisogna fare almeno un tentativo e terminare la partita");
                                                        break;
                                                    case -3 :
                                                        JOptionPane.showMessageDialog(null, "Errore. Utente non ha effettuato il login");
                                                        break;
                                                }
                                            }
                                            catch (Exception ee) {ee.printStackTrace();}
                                        }
                                    });
                                    ShowMeRancking.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {

                                            try {
                                                DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                                                DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                                                ou.writeInt((("showMeRanking:" + usernamelogin).length())*2);
                                                ou.writeChars("showMeRanking:" + usernamelogin);
                                                ou.flush();
                                                System.out.print(inn.readInt());


                                                switch(inn.readInt()) {
                                                    case 0 :

                                                        JFrame seePodio = new JFrame("CLASSIFICA");
                                                        seePodio.setLayout(new BorderLayout());
                                                        seePodio.setLocation(new Point(300, 300));

                                                        JTextArea info = new JTextArea();
                                                        info.setEditable(false);
                                                        JScrollPane scrll = new JScrollPane(info);


                                                        int lenDati = inn.readInt();//recupero la lunghezza dei dati
                                                        //qui posso usare il metodo readData credo, in caso vedere
                                                        String classifica = new String();

                                                        for(int i = 0; i<lenDati; i++) {classifica = classifica.concat(String.valueOf(inn.readChar())); }

                                                        info.append(classifica);

                                                        seePodio.add(info, BorderLayout.CENTER);
                                                        seePodio.setSize(200, 200);
                                                        seePodio.setVisible(true);

                                                        break;
                                                    case -1:
                                                        JOptionPane.showMessageDialog(null, "Errore. Impossibile visualizzare la classifica");
                                                        break;
                                                }
                                            }
                                            catch (Exception ee) {ee.printStackTrace();}

                                        }
                                    });
                                    Visualizza.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {

                                            if(Classifica.getText().equals("Nuova notifica")) {

                                                //recupero il podio
                                                ArrayList<UserValoreClassifica> ClassNotifica = notifica.GetClassififca();
                                                JFrame seePodio = new JFrame("PODIO");
                                                seePodio.setLayout(new BorderLayout());

                                                seePodio.setLocation(new Point(300, 300));

                                                JTextArea info = new JTextArea();
                                                info.setEditable(false);
                                                info.setRows(3);
                                                JScrollPane scrll = new JScrollPane(info);
                                                for(int i = 0; i<ClassNotifica.size(); i++) {
                                                    info.append("UTENTE: " + ClassNotifica.get(i).getUsername() + " SCORE: " + ClassNotifica.get(i).getScore() + "\n");
                                                }
                                                seePodio.add(info, BorderLayout.CENTER);
                                                seePodio.setSize(200, 200);
                                                seePodio.setVisible(true);
                                                Classifica.setText("Nessuna Notifica");

                                            }
                                            else {
                                                JOptionPane.showMessageDialog(null, "Nessuna notifica da visualizzare");
                                            }
                                        }
                                    });
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
                                                System.out.print(inn.readInt()+ "2");

                                                switch(inn.readInt()) {
                                                    case 0 :

                                                        servizio.UnRegisryForCallBack(user, skeleton);
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
                                                System.out.print(inn.readInt()+ "3");

                                                switch(inn.readInt()) {
                                                    case 0 :
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
                                                    System.out.println(result+ "5");
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
                                                        case -5 :
                                                            JOptionPane.showMessageDialog(null, "Utente non ha effettuato il login");
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
                                                if(statistic != null) JOptionPane.showMessageDialog(null, statistic);
                                                else JOptionPane.showMessageDialog(null, "Impossibile visualizzare le statistiche");
                                            }
                                            catch (Exception ee) {ee.printStackTrace();}
                                            //-----------------------------------------------------//
                                        }
                                    });

                                    mainPanel.add(makePanelLogout(Logout));
                                    mainPanel.add(makePanelPlayStart(Gioca));
                                    mainPanel.add(makePanelSend(SendWord));
                                    mainPanel.add(makePanelStatistics(sendMeStatistics));
                                    mainPanel.add(makeSeeNotify(Visualizza));
                                    mainPanel.add(makePanelShowMeRanking(ShowMeRancking));
                                    mainPanel.add(makePanelShare(Share));
                                    mainPanel.add(makePanelNextWord(TimeNextWord));
                                    mainPanel.add(ShowMeSharing);

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
                            System.out.println("CATCH ESTERNO");
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
                            //Registrazione servizio = (Registrazione) LocateRegistry.getRegistry(6500).lookup("Registrazione");

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
    private JPanel makePanelShare(JButton Share) {

        JPanel panelShare = new JPanel();
        panelShare.setLayout(new BoxLayout(panelShare, BoxLayout.Y_AXIS));
        panelShare.add(new JLabel("Condividi Risultati: "));
        panelShare.add(new JLabel(" "));
        panelShare.add(new JLabel(" "));
        panelShare.add(new JLabel(" "));

        panelShare.add(Share);

        return panelShare;
    }
    private JPanel makePanelShowMeRanking(JButton ShowMeRancking) {

        JPanel panelShowRanking = new JPanel();
        panelShowRanking.setLayout(new BoxLayout(panelShowRanking, BoxLayout.Y_AXIS));
        panelShowRanking.add(new JLabel("Visualizza classifica attuale: "));
        panelShowRanking.add(new JLabel(" "));
        panelShowRanking.add(new JLabel(" "));
        panelShowRanking.add(new JLabel(" "));

        panelShowRanking.add(ShowMeRancking);

        return panelShowRanking;
    }
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
    private JPanel makePanelNextWord(JButton TimeNextWord) {

        JPanel panelNextWord = new JPanel();

        panelNextWord.setLayout(new BoxLayout(panelNextWord, BoxLayout.Y_AXIS));

        panelNextWord.add((new JLabel(" ")));
        panelNextWord.add(new JLabel("StartSession:"));
        panelNextWord.add((new JLabel(" ")));
        panelNextWord.add((NextWordLable = new JLabel("Unknown")));
        panelNextWord.add(TimeNextWord);

        return panelNextWord;

    }
    private JPanel makePanelPlayStart(JButton play) {

        JPanel panelPlay = new JPanel();

        panelPlay.setLayout(new BoxLayout(panelPlay, BoxLayout.Y_AXIS));
        panelPlay.add((new JLabel("Inizia partita: ")));
        panelPlay.add(play);

        return panelPlay;
    }
    private JPanel makeSeeNotify(JButton visualizza) {

        JPanel panelNotify = new JPanel();

        panelNotify.setLayout(new BoxLayout(panelNotify, BoxLayout.Y_AXIS));
        panelNotify.add(new JLabel("Aggiornamento classifica:\n"));
        panelNotify.add(new JLabel(" "));
        panelNotify.add(new JLabel(" "));
        panelNotify.add(Classifica);
        panelNotify.add(visualizza);

        return panelNotify;
    }
    public JPanel MakeAllSuggestionsPanel(Suggerimenti dati) {

        //in lst sono contenuti tutti i sugerimenti di un utente
        //quindi nel main panel devo inserire prima il nome dell utente
        //il metodo corrente viene richiamato in un while e viene aggiunto piu volte
        //questo main panel che ritorno cosi riesco ad inserire tutti i risultati di tutti gli utenti
        GridLayout layoutMain = null;
        JPanel main = new JPanel();
        main.setLayout(layoutMain = new GridLayout(0, 1));
        layoutMain.setVgap(10);

        main.setBorder(BorderFactory.createTitledBorder(dati.getUtente()));
        ArrayList<String> lst = dati.getSuggerimenti();

        for(int i = 0; i< lst.size(); i++) {

            String suggerimento = lst.get(i);
            System.out.println(suggerimento);
            JPanel panel = new JPanel(new GridLayout(1, 10));

            for(int j = 0; j<10; j++) {//suggestions è lunga 10

                JLabel label = new JLabel(String.valueOf('*'), SwingConstants.CENTER);
                label.setOpaque(true);

                switch (String.valueOf(suggerimento.charAt(j))) {

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
            main.add(panel);
        }
        return main;
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
            else return null;
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
