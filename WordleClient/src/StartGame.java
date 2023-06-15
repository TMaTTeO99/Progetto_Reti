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
import java.util.ArrayList;
import java.util.Date;
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

    private Registrazione servizio = null;
    public StartGame(String IP_server, int Port_listening, String IP_Multicast, int Port_Multicast, int PortRMI) throws Exception {

        //recupero il servizio di registrazione
        servizio = (Registrazione) LocateRegistry.getRegistry(PortRMI).lookup("Registrazione");

        // Configurazione del frame principale
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

                        usernamelogin = UserTEXTLogin.getText();
                        String pass = new String(UserTEXTpasslogin.getPassword());//qui prima era getText, l ho modificato

                        if(usernamelogin.length() == 0 || pass.length() == 0)return -4;
                        try {
                            DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                            DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                            ou.writeInt((("login:"+ usernamelogin + " " + pass).length())*2);
                            ou.writeChars("login:" + usernamelogin + " " + pass);
                            ou.flush();
                            inn.readInt(); //scarto la len del messaggio

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

                        return returnvalue;
                    }
                    @Override
                    protected void done() {
                        try {

                            Integer response = get();  // Recupero il valore di ritorno del metodo doInBackground
                            switch (response) {

                                case 1 ://caso in cui la richiesta di login è stata completata

                                    // Chiudo il frame corrente e apro il nuovo frame
                                    dispose();

                                    JFrame Frame = new JFrame("Wordle Game");
                                    Frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                    Frame.setLocation(new Point(200, 200));

                                    JPanel mainPanel = new JPanel();
                                    mainPanel.setLayout(new GridLayout(0, 2));


                                    JButton ShowMeSharing = new JButton("See");
                                    JButton Visualizza = new JButton("See");
                                    JButton Logout = new JButton("Logout");
                                    JButton Gioca = new JButton("Gioca");
                                    JButton SendWord = new JButton("Send");
                                    JButton sendMeStatistics = new JButton("sendMeStatistics");
                                    JButton ShowMeRancking = new JButton("showMeRanking");
                                    JButton Share = new JButton("Share");
                                    JButton TimeNextWord = new JButton("TimeNextWord");
                                    JButton help = new JButton("Help");

                                    //a questo punto quello che faccio è lanciare un thread che sta i ascolto
                                    //dei dati che vengono inviati dal server sul gruppo multicast
                                    //porta e ip dovranno essere dati presi dalfile di config
                                    Thread multiCast = new Thread(new CaptureUDPmessages(IP_Multicast, Port_Multicast, SuggerimentiQueueTemp, locksuggerimenti));
                                    multiCast.start();

                                    //Aggiungo gli ascoltatori di azioni ai JButton
                                    help.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {

                                            JOptionPane.showMessageDialog(null, "Oltre alle ovvie operazioni il gioco consente anche di: \n" +
                                                    "1) Visualizzare le prime 3 posizioni della classifica quando vengono aggiornate (NOTIFICHE AGGIORNAMENTO CLASSIFICA)\n" +
                                                    "2) Condividere il risultato della partita (CONDIVIDI RISULTATI)\n" +
                                                    "3) Visualizzare le condivisioni degli altri utenti (VISUALIZZA CONDIVISIONI UTENTI)\n" +
                                                    "4) Visualizzare le proprie statistiche (STATISTICHE)\n" +
                                                    "5) Visualizzare la data e l ora in cui la parola corrente verrà aggiornata (START SESSION)");

                                        }
                                    });
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

                                                        //aggiorno la data di quando verra rilasciata la prossima parola da indovinare
                                                        DataNextWord = new Date(System.currentTimeMillis() + Long.parseLong(ReadData(inn)));
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
                                                inn.readInt();//scarto la len del messaggio

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
                                                inn.readInt();//scarto la len del messaggio


                                                switch(inn.readInt()) {
                                                    case 0 :

                                                        JFrame seePodio = new JFrame("CLASSIFICA");
                                                        seePodio.setLayout(new BorderLayout());
                                                        seePodio.setLocation(new Point(300, 300));

                                                        JTextArea info = new JTextArea();
                                                        info.setEditable(false);
                                                        JScrollPane scrll = new JScrollPane(info);

                                                        info.append(ReadData(inn));

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
                                                inn.readInt();//scarto la len del messaggio

                                                switch(inn.readInt()) {
                                                    case 0 :
                                                        servizio.UnRegisryForCallBack(user, skeleton);
                                                        UnicastRemoteObject.unexportObject(notifica, true);
                                                        Frame.dispose();
                                                        new StartGame(IP_server, Port_listening, IP_Multicast, Port_Multicast, PortRMI);
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
                                                inn.readInt();//scarto la len del messaggio

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
                                                    case -3 :
                                                        JOptionPane.showMessageDialog(null, "ERROE. L'utente non ha effettuato il login");
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

                                                    inn.readInt();//scarto la len del messaggio

                                                    int result = inn.readInt();
                                                    String wordTradotta = null;

                                                    switch(result) {
                                                        case 2 ://caso in cui ho sfruttato l ultimo tentativo e ho perso
                                                            //devo recuperare la parola tradotta

                                                            wordTradotta = ReadData(inn);
                                                            JOptionPane.showMessageDialog(null, "Tentativi terminati\n Traduzione: " + wordTradotta);


                                                            break;
                                                        case 1 ://caso in cui devo ricevere i suggerimenti

                                                            String sug = ReadData(inn);//recupero i suggerimenti

                                                            //a questo punto quello che devo fare è visualizzare i suggerimenti in forma grafica
                                                            JPanel suggestionPanle = MakeSuggestionsPanel(sug, word);
                                                            JOptionPane.showMessageDialog(null, suggestionPanle, "Suggerimenti", JOptionPane.PLAIN_MESSAGE);

                                                            break;
                                                        case 0 ://caso in cui la parola è stata indovinata
                                                            // In questo caso lato server dovro inserire la
                                                            // traduzione della parola che qui andra letta

                                                            wordTradotta = ReadData(inn);
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

                                            try {
                                                DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                                                DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                                                ou.writeInt((("sendMeStatistics:"+ usernamelogin).length())*2);
                                                ou.writeChars("sendMeStatistics:" + usernamelogin);
                                                ou.flush();
                                                inn.readInt();//scarto la lunghezza del messaggio

                                                if(inn.readInt() == 0){//controllo eventuale messaggio di errore che il server invia
                                                    String statistic = ReadData(inn);
                                                    JOptionPane.showMessageDialog(null, statistic);
                                                }
                                                else JOptionPane.showMessageDialog(null, "Impossibile visualizzare le statistiche");

                                            }
                                            catch (Exception ee) {ee.printStackTrace();}
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
                                    mainPanel.add(makePanelShowMeShareing(ShowMeSharing));
                                    mainPanel.add(makeHelpButton(help));

                                    mainPanel.setBackground(new Color(92, 89, 94));
                                    Frame.add(mainPanel, BorderLayout.CENTER);//prima non c'era BorderLayout.CENTER
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
        return panelShowRules;

    }
    private JPanel makeHelpButton(JButton help) {

        JPanel panelHelp = new JPanel();
        panelHelp.setPreferredSize(new Dimension(20, 20));
        panelHelp.setBorder(BorderFactory.createTitledBorder("VISUALIZZA AIUTI: "));
        panelHelp.setLayout(new BoxLayout(panelHelp, BoxLayout.Y_AXIS));
        panelHelp.add(help);
        panelHelp.setBackground(new Color(192, 166, 209));
        return panelHelp;
    }
    private JPanel makePanelShowMeShareing(JButton ShowMeSharing) {

        JPanel panelShowShare = new JPanel();
        panelShowShare.setPreferredSize(new Dimension(20, 20));
        panelShowShare.setBorder(BorderFactory.createTitledBorder("VISUALIZZA CONDIVISIONI UTENTI: "));
        panelShowShare.setLayout(new BoxLayout(panelShowShare, BoxLayout.Y_AXIS));
        panelShowShare.add(ShowMeSharing);
        panelShowShare.setBackground(new Color(192, 166, 209));
        return panelShowShare;


    }
    private JPanel makePanelShare(JButton Share) {

        JPanel panelShare = new JPanel();
        panelShare.setPreferredSize(new Dimension(20, 20));
        panelShare.setBorder(BorderFactory.createTitledBorder("CONDIVIDI RISULTATI: "));
        panelShare.setLayout(new BoxLayout(panelShare, BoxLayout.Y_AXIS));
        panelShare.setBackground(new Color(192, 166, 209));
        panelShare.add(Share);

        return panelShare;
    }
    private JPanel makePanelShowMeRanking(JButton ShowMeRancking) {

        JPanel panelShowRanking = new JPanel();
        panelShowRanking.setPreferredSize(new Dimension(20, 20));
        panelShowRanking.setBorder(BorderFactory.createTitledBorder("VISUALIZZA CLASSIFICA: "));
        panelShowRanking.setLayout(new BoxLayout(panelShowRanking, BoxLayout.Y_AXIS));
        panelShowRanking.setBackground(new Color(192, 166, 209));
        panelShowRanking.add(ShowMeRancking);

        return panelShowRanking;
    }
    private JPanel makePanelStatistics(JButton sendMeStatistics) {

        JPanel panelStatistics = new JPanel();
        panelStatistics.setPreferredSize(new Dimension(20, 20));
        panelStatistics.setBorder(BorderFactory.createTitledBorder("STATISTICHE: "));
        panelStatistics.setLayout(new BoxLayout(panelStatistics, BoxLayout.Y_AXIS));
        panelStatistics.setBackground(new Color(192, 166, 209));
        panelStatistics.add(sendMeStatistics);

        return panelStatistics;
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
        return panelRegistra;
    }
    private JPanel makePanelLogout(JButton log) {


        JPanel panelLogout = new JPanel();
        panelLogout.setPreferredSize(new Dimension(20, 20));
        panelLogout.setBorder(BorderFactory.createTitledBorder("LOGOUT: "));
        TExtFieldUserLogout = new JTextField(10);

        //utilizzo una classe anonima per poter implementare un actionPerformed in modo da consentire all utente di usare il tasto invio
        //per eseguire il tasto Logout
        TExtFieldUserLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.doClick();
            }
        });

        panelLogout.setLayout(new BoxLayout(panelLogout, BoxLayout.Y_AXIS));
        panelLogout.add(new JLabel("Username: " + usernamelogin));//in caso un utente non ricordasse con quale username si è loggato
        panelLogout.add(TExtFieldUserLogout);
        panelLogout.setBackground(new Color(192, 166, 209));
        panelLogout.add(log);

        return panelLogout;

    }
    private JPanel makePanelSend(JButton log) {

        JPanel panelSend = new JPanel();
        panelSend.setPreferredSize(new Dimension(20, 20));
        panelSend.setBorder(BorderFactory.createTitledBorder("SEND WORD: "));
        TextFieldWordSendWord = new JTextField(10);

        //utilizzo una classe anonima per poter implementare un actionPerformed in modo da consentire all utente di usare il tasto invio
        //per eseguire il tasto Send
        TextFieldWordSendWord.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.doClick();
            }
        });

        panelSend.setLayout(new BoxLayout(panelSend, BoxLayout.Y_AXIS));
        panelSend.add(TextFieldWordSendWord);
        panelSend.add(log);
        panelSend.setBackground(new Color(192, 166, 209));
        return panelSend;

    }
    private JPanel makePanelNextWord(JButton TimeNextWord) {

        JPanel panelNextWord = new JPanel();
        panelNextWord.setPreferredSize(new Dimension(20, 20));
        panelNextWord.setBorder(BorderFactory.createTitledBorder("START SESSION: "));
        panelNextWord.setLayout(new BoxLayout(panelNextWord, BoxLayout.Y_AXIS));

        panelNextWord.add((NextWordLable = new JLabel("Unknown")));
        panelNextWord.add(TimeNextWord);
        panelNextWord.setBackground(new Color(192, 166, 209));
        return panelNextWord;

    }
    private JPanel makePanelPlayStart(JButton play) {

        JPanel panelPlay = new JPanel();
        panelPlay.setPreferredSize(new Dimension(20, 20));
        panelPlay.setBorder(BorderFactory.createTitledBorder("INIZIA PARTITA: "));

        panelPlay.setLayout(new BoxLayout(panelPlay, BoxLayout.Y_AXIS));
        panelPlay.add(play);
        panelPlay.setBackground(new Color(192, 166, 209));;
        return panelPlay;
    }
    private JPanel makeSeeNotify(JButton visualizza) {

        JPanel panelNotify = new JPanel();
        panelNotify.setPreferredSize(new Dimension(20, 20));
        panelNotify.setBorder(BorderFactory.createTitledBorder("NOTIFICHE AGGIORNAMENTO CLASSIFICA: "));
        panelNotify.setLayout(new BoxLayout(panelNotify, BoxLayout.Y_AXIS));
        panelNotify.add(Classifica);
        panelNotify.add(visualizza);
        panelNotify.setBackground(new Color(192, 166, 209));

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
    private String ReadData(DataInputStream inn) {

        char [] data = null;
        try {
            int read = 0, len = inn.readInt();
            data = new char[len];
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