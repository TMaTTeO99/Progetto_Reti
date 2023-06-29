import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class StartGame extends JFrame {

    private int Port_Multicast;
    private int PortRMI;
    private String IP_Multicast;
    private String usernamelogin;
    private String word;//stringa che conterrà la parola che l utente inserisce
    private Socket socket;
    private Registrazione servizio = null;//variabile usata per registrare il client per la callback visto che questa classe si avvia dopo il login
    private NotificaClient skeleton;//skeleton che il client invia al server tramite il servizio offerto dal server per poi ricevere le notifiche
    private ImplementazioneNotificaClient notifica;//oggetto che implementa veramente la classe per poter recuperare le notifiche del client
    private JTextField TExtFieldUserLogout;
    private JTextField TextFieldWordSendWord;
    private JLabel NextWordLable;
    private JLabel Classifica = new JLabel("Nessuna Notifica");//lable per visualizzare se arrivano callback
    private Date DataNextWord = new Date(0);
    private ArrayList<Suggerimenti> SuggerimentiQueue;//coda che conterra i suggerimenti che gli altri client condividono
    private ReentrantLock locksuggerimenti = new ReentrantLock();//lock usata per implementare mutua esclusione sulla coda dei suggerimenti
    private MulticastSocket sockMulticast;
    private InetSocketAddress addressMulticat;
    private Thread multiCastThread;//thread usato per recuperare le condivisioni dagli utenti
    private GetDataConfig dataConfig;
    private UUID ID_Channel;//id che il server associa alla connessione
    private String SecurityKey;//chiave di sessione
    private JFrame LGFrame;
    public StartGame(GetDataConfig dataCon, Socket sck,
                     String usrname, Registrazione srv,
                     ArrayList<Suggerimenti> SuggQueue,
                     String ScrtKey, UUID ID, JFrame LGF) throws Exception{

        LGFrame = LGF;
        SecurityKey = ScrtKey;
        dataConfig = dataCon;
        IP_Multicast = dataConfig.getIP_Multicast();
        Port_Multicast = dataConfig.getPort_Multicast();
        PortRMI = dataConfig.getPortExport();
        socket = sck;
        usernamelogin = usrname;
        servizio = srv;
        SuggerimentiQueue = SuggQueue;
        ID_Channel = ID;

        //Mi registro per il servizio di notifica
        notifica = new ImplementazioneNotificaClient(Classifica);
        skeleton = (NotificaClient) UnicastRemoteObject.exportObject(notifica, 0);
        servizio.RegisryForCallBack(usernamelogin, skeleton, ID_Channel);



        setTitle("Wordle Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocation(new Point(200, 200));

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
        JButton Help = new JButton("Help");

        //a questo punto quello che faccio è lanciare un thread che sta i ascolto
        //dei dati che vengono inviati dal server sul gruppo multicast
        sockMulticast = new MulticastSocket(addressMulticat = new InetSocketAddress(IP_Multicast, Port_Multicast));//creo la socket
        sockMulticast.joinGroup(addressMulticat, null);//non specifico nessuna interfaccia di rete per essere piu generico possibile e mi unisco
        multiCastThread = new Thread(new CaptureUDPmessages(sockMulticast, SuggerimentiQueue, locksuggerimenti));
        multiCastThread.start();//lancio il thread che sta in ascolto

        //aggiungo a Jbutton tutte le operazioni che devono fare quando vengono usati dall utente
        //uso le espressioni lambda per evitare di creare delle classi normali o anonime per ogni jbutton
        Help.addActionListener(e -> {AddHelpSetUp();});
        TimeNextWord.addActionListener(e -> {AddTimeNextWordSetUp();});
        ShowMeSharing.addActionListener(e -> {AddShowMeSharing();});
        Share.addActionListener(e -> {AddShare();});
        ShowMeRancking.addActionListener(e -> {AddShowMeRancking();});
        Visualizza.addActionListener(e -> {AddVisualizza();});
        Logout.addActionListener(e -> {AddLogout();});
        Gioca.addActionListener(e -> {AddGioca();});
        SendWord.addActionListener(e -> {AddSendWord();});
        sendMeStatistics.addActionListener(e -> {AddStatistics();});

        //aggiungo i Jpanel al Jpanel principale
        mainPanel.add(makePanelLogout(Logout));
        mainPanel.add(makePanelPlayStart(Gioca));
        mainPanel.add(makePanelSend(SendWord));
        mainPanel.add(makePanelStatistics(sendMeStatistics));
        mainPanel.add(makeSeeNotify(Visualizza));
        mainPanel.add(makePanelShowMeRanking(ShowMeRancking));
        mainPanel.add(makePanelShare(Share));
        mainPanel.add(makePanelNextWord(TimeNextWord));
        mainPanel.add(makePanelShowMeShareing(ShowMeSharing));
        mainPanel.add(makeHelpButton(Help));

        mainPanel.setBackground(new Color(92, 89, 94));
        add(mainPanel, BorderLayout.CENTER);//prima non c'era BorderLayout.CENTER
        setSize(1000, 500);
        setVisible(true);

    }
    private void AddShowMeRancking() {

        SwingWorker<ReturnPackage, Void> worker = new SwingWorker<ReturnPackage, Void>() {

            @Override
            protected ReturnPackage doInBackground() throws Exception {

                int returnValue = Integer.MAX_VALUE;//MAX_VALUE valore di inizializzazione
                ReturnPackage pckage = null;
                try {

                    DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    ou.writeInt((("showMeRanking:" + usernamelogin).length())*2);
                    ou.writeChars("showMeRanking:" + usernamelogin);
                    ou.flush();
                    inn.readInt();//scarto la len del messaggio
                    returnValue = inn.readInt();
                    pckage = new ReturnPackage(returnValue, inn);
                }
                catch (Exception ee) {pckage = new ReturnPackage(-10);}
                return pckage;
            }

            @Override
            protected void done() {

                try {
                    ReturnPackage pckage = get();
                    int returnValue = pckage.getReturnValue();
                    switch(returnValue) {
                        case 0 :

                            JFrame seePodio = new JFrame("CLASSIFICA");
                            seePodio.setLayout(new BorderLayout());
                            seePodio.setLocation(new Point(800, 300));

                            JTextArea info = new JTextArea();
                            info.setEditable(false);
                            JScrollPane scrll = new JScrollPane(info);


                            seePodio.add(scrll, BorderLayout.CENTER);
                            info.append(ReadData(pckage.getInn()));
                            seePodio.setSize(250, 300);
                            seePodio.setVisible(true);

                            break;
                        case -1:
                            JOptionPane.showMessageDialog(null, "Errore. Impossibile visualizzare la classifica");
                            break;
                        case -10:
                            JOptionPane.showMessageDialog(null, "Errore server");
                            break;
                    }
                }
                catch (Exception e) {e.printStackTrace();}

            }
        };
        worker.execute();
    }
    private void AddShare() {


        SwingWorker<ReturnPackage, Void> worker = new SwingWorker<ReturnPackage, Void>() {

            @Override
            protected ReturnPackage doInBackground() throws Exception{

                int returnValue = Integer.MAX_VALUE;//MAX_VALUE valore di inizializzazione
                try {
                    DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    ou.writeInt((("share:"+ usernamelogin).length())*2);
                    ou.writeChars("share:" + usernamelogin);
                    ou.flush();
                    inn.readInt();//scarto la len del messaggio
                    returnValue = inn.readInt();
                }
                catch (Exception ee) {returnValue = -10;}
                return new ReturnPackage(returnValue);
            }
            @Override
            protected void done() {

                try {

                    int returnValue = get().getReturnValue();

                    switch(returnValue) {
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
                        case -10:
                            JOptionPane.showMessageDialog(null, "Errore server");
                            break;
                    }
                }
                catch (Exception e) {e.printStackTrace();}

            }

        };
        worker.execute();

    }
    private void AddShowMeSharing() {

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {

                JFrame shareFrame = new JFrame("SUGGERIMENTI CONDIVISI");
                JPanel main = new JPanel();

                main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

                shareFrame.setLayout(new BoxLayout(shareFrame.getContentPane(), BoxLayout.Y_AXIS));
                shareFrame.setLocation(new Point(300, 300));

                try {
                    locksuggerimenti.lock();
                    if(SuggerimentiQueue.size() != 0) {

                        int i = 0;

                        while(i < SuggerimentiQueue.size()) {

                            //Aggiungo i suggerimenti a un unico mainpanel per poterli visualizzare
                            main.add(MakeAllSuggestionsPanel(SuggerimentiQueue.get(i)));
                            i++;
                        }

                        JScrollPane scrollBar = new JScrollPane(main);
                        scrollBar.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                        shareFrame.setContentPane(scrollBar);
                        shareFrame.setSize(200, 200);
                        shareFrame.setVisible(true);
                    }
                    else {
                        JOptionPane.showMessageDialog(null, "Nessuna Notifica");
                    }
                }
                catch (Exception ex) {JOptionPane.showMessageDialog(null, "Errore");}
                finally {locksuggerimenti.unlock();}


                return null;
            }
        };
        worker.execute();
    }
    private void AddTimeNextWordSetUp() {

        SwingWorker<ReturnPackage, Void> worker = new SwingWorker<ReturnPackage, Void> () {

            @Override
            protected ReturnPackage doInBackground() {

                ReturnPackage pckage = null;
                int returnValue = Integer.MAX_VALUE;//MAX_VALUE valore di inizializzazione
                try {
                    DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    ou.writeInt((("TimeNextWord:" + usernamelogin).length())*2);
                    ou.writeChars("TimeNextWord:" + usernamelogin);
                    ou.flush();
                    inn.readInt();;//scarto la lunghezza del messaggio
                    returnValue = inn.readInt();
                    pckage = new ReturnPackage(returnValue, inn);
                }
                catch (Exception ee) {pckage = new ReturnPackage(-1);}
                return pckage;
            }
            @Override
            protected void done() {
                int returnValue = Integer.MAX_VALUE;//MAX_VALUE valore di inizializzazione

                try {

                    ReturnPackage pckage = get();
                    returnValue = pckage.getReturnValue();//recupero l eventuiiale valore di errore

                    switch (returnValue) {
                        case 0 :
                            //aggiorno la data di quando verra rilasciata la prossima parola da indovinare
                            DataNextWord = new Date(System.currentTimeMillis() + Long.parseLong(ReadData(pckage.getInn())));
                            NextWordLable.setText(""+DataNextWord);
                            break;
                        default :
                            JOptionPane.showMessageDialog(null, "ERRORE");
                            break;
                    }
                }
                catch (Exception e) {

                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    private void AddHelpSetUp() {

        SwingWorker <Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception{

                JOptionPane.showMessageDialog(null, "Oltre alle ovvie operazioni il gioco consente anche di: \n" +
                        "1) Visualizzare le prime 3 posizioni della classifica quando vengono aggiornate (NOTIFICHE AGGIORNAMENTO CLASSIFICA)\n" +
                        "2) Condividere il risultato della partita (CONDIVIDI RISULTATI)\n" +
                        "3) Visualizzare le condivisioni degli altri utenti (VISUALIZZA CONDIVISIONI UTENTI)\n" +
                        "4) Visualizzare le proprie statistiche (STATISTICHE)\n" +
                        "5) Visualizzare la data e l ora in cui la parola corrente verrà aggiornata (START SESSION)");

                return 0;
            }
        };
        worker.execute();

    }
    private void AddVisualizza() {

        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {

            @Override
            protected Integer doInBackground() {

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
                    //inserisco nella text area i dati degli utenti presenti nella classifica
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
                return 0;
            }
        };
        worker.execute();

    }
    private void AddLogout() {

        SwingWorker<ReturnPackage, Void> worker = new SwingWorker<ReturnPackage, Void>() {

            @Override
            protected ReturnPackage doInBackground() throws Exception {

                int returnValue = Integer.MAX_VALUE;//MAX_VALUE valore di inizizalizzazione
                String user = TExtFieldUserLogout.getText();
                ReturnPackage pckage = null;
                try {
                    DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    ou.writeInt((("logout:"+ user).length())*2);
                    ou.writeChars("logout:" + user);
                    ou.flush();
                    inn.readInt();//scarto la len del messaggio
                    returnValue = inn.readInt();
                    pckage = new ReturnPackage(returnValue);

                    if(returnValue == 0) {
                        servizio.UnRegisryForCallBack(user, skeleton, ID_Channel);
                        UnicastRemoteObject.unexportObject(notifica, true);
                    }

                }
                catch (Exception ee) {
                    pckage = new ReturnPackage(-10);//caso di errore generico
                }

                return pckage;
            }

            @Override
            protected void done() {

                try {
                    int returnValue = get().getReturnValue();
                    switch(returnValue) {
                        case 0 :

                            StopCaptureUDPMessages();//metodo privato per la terminazione del thread che sta in ascolo dei datagramPacket
                            //torno al frame iniziale della login e della registrazione
                            setVisible(false);
                            LGFrame.setVisible(true);
                            break;
                        case -1:
                            JOptionPane.showMessageDialog(null, "Errore. Username inserito non corretto");
                            break;
                        case -2:
                            JOptionPane.showMessageDialog(null, "Errore. Per effettuare il logout bisogna prima aver effettuato il login");
                            break;
                        case -3:
                            JOptionPane.showMessageDialog(null, "Errore. Inserire il proprio username");
                            break;
                        case -4:
                            JOptionPane.showMessageDialog(null, "Errore");
                            break;
                        case -10:
                            JOptionPane.showMessageDialog(null, "Errore server");
                            break;
                    }
                }
                catch (Exception e) {e.printStackTrace();}
            }
        };
        worker.execute();

    }
    private void AddGioca() {

        SwingWorker<ReturnPackage, Void> worker = new SwingWorker<ReturnPackage, Void>() {
            @Override
            protected ReturnPackage doInBackground() throws Exception {

                int returnValue = Integer.MAX_VALUE;//MAX_VALUE valore di defaul
                ReturnPackage pckage = null;
                try {
                    DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    ou.writeInt((("playWORDLE:"+ usernamelogin).length())*2);
                    ou.writeChars("playWORDLE:" + usernamelogin);
                    ou.flush();

                    inn.readInt();//scarto la len del messaggio
                    returnValue = inn.readInt();
                    pckage = new ReturnPackage(returnValue);
                }
                catch (Exception ee) {pckage = new ReturnPackage(-10);}//errore generico
                return pckage;

            }
            @Override
            protected void done() {

                try {

                    int returnValue = get().getReturnValue();

                    switch(returnValue) {
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
                        case -4:
                            JOptionPane.showMessageDialog(null, "ERROE.");
                            break;
                        case -10:
                            JOptionPane.showMessageDialog(null, "Errore server");
                            break;
                    }
                }
                catch (Exception e) {e.printStackTrace();}
            }
        };
        worker.execute();

    }
    private void AddSendWord() {

        SwingWorker<ReturnPackage, Void> worker = new SwingWorker<>() {

            @Override
            protected ReturnPackage doInBackground() throws Exception {

                int returnValue = Integer.MAX_VALUE;//MAX_VALUE valore di inizializzazione
                ReturnPackage pckage = null;
                word = TextFieldWordSendWord.getText();

                if(word.length() == 0) {return new ReturnPackage(-6);}//caso in cui l utente non inserisce la parola
                else {

                    try {
                        DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                        DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                        byte [] dataCipeherd = SecurityClass.encrypt(usernamelogin + " " + word, SecurityKey);

                        ou.writeInt(dataCipeherd.length + (("sendWord:").length())*2);
                        ou.writeChars("sendWord:");
                        ou.write(dataCipeherd, 0, dataCipeherd.length);
                        ou.flush();

                        inn.readInt();//scarto la len del messaggio
                        returnValue = inn.readInt();//recupero il valore di ritorno

                        pckage = new ReturnPackage(returnValue, inn);


                    }
                    catch (Exception ee) {pckage = new ReturnPackage(-10);}
                }
                return pckage;
            }

            @Override
            protected void done() {

                try {
                    String wordTradottaAndStatistics = null;
                    ReturnPackage pckage = get();
                    int returnValue = pckage.getReturnValue();

                    switch(returnValue) {
                        case 2 ://caso in cui ho sfruttato l ultimo tentativo e ho perso
                                //devo recuperare la parola tradotta e le statistiche

                            wordTradottaAndStatistics = SecurityClass.decrypt(ReadDataByte(pckage.getInn()), SecurityKey);
                            JOptionPane.showMessageDialog(null, "Tentativi terminati\n Traduzione: " + wordTradottaAndStatistics);

                            break;
                        case 1 ://caso in cui devo ricevere i suggerimenti

                            String sug = SecurityClass.decrypt(ReadDataByte(pckage.getInn()), SecurityKey);//recupero i suggerimenti

                            //a questo punto quello che devo fare è visualizzare i suggerimenti in forma grafica
                            JPanel suggestionPanle = MakeSuggestionsPanel(sug, word);
                            JOptionPane.showMessageDialog(null, suggestionPanle, "Suggerimenti", JOptionPane.PLAIN_MESSAGE);

                            break;
                        case 0 ://caso in cui la parola è stata indovinata

                            wordTradottaAndStatistics = SecurityClass.decrypt(ReadDataByte(pckage.getInn()), SecurityKey);
                            JOptionPane.showMessageDialog(null, "Vittoria\nTraduzione: " + wordTradottaAndStatistics);

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
                        case -6 :
                            JOptionPane.showMessageDialog(null, "Errore. Inserire parola");
                            break;
                        case -7 :
                            JOptionPane.showMessageDialog(null, "Errore");
                            break;
                        case -10:
                            JOptionPane.showMessageDialog(null, "Errore server");
                            break;
                    }
                }
                catch (Exception e){e.printStackTrace();}
            }
        };
        worker.execute();
    }
    private void AddStatistics() {

        SwingWorker<ReturnPackage, Void> worker = new SwingWorker<ReturnPackage, Void>() {
            @Override
            protected ReturnPackage doInBackground() throws Exception {

                int returnValue = Integer.MAX_VALUE;//MAX_VALUE valore di inizializzazione
                ReturnPackage pckage = null;
                try {
                    DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    ou.writeInt((("sendMeStatistics:"+ usernamelogin).length())*2);
                    ou.writeChars("sendMeStatistics:" + usernamelogin);
                    ou.flush();
                    inn.readInt();//scarto la lunghezza del messaggio
                    returnValue = inn.readInt();
                    pckage = new ReturnPackage(returnValue, inn);

                }
                catch (Exception ee) {pckage = new ReturnPackage(-1);}

                return pckage;
            }
            @Override
            protected void done() {

                try {

                    ReturnPackage pckage = get();
                    if(pckage.getReturnValue() == 0){//controllo eventuale messaggio di errore che il server invia
                        String statistic = SecurityClass.decrypt(ReadDataByte(pckage.getInn()), SecurityKey);
                        JOptionPane.showMessageDialog(null, statistic);
                    }
                    else JOptionPane.showMessageDialog(null, "Impossibile visualizzare le statistiche");

                }
                catch (Exception e) {e.printStackTrace();}
            }
        };
        worker.execute();
    }

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
    private JPanel makePanelLogout(JButton log) {


        JPanel panelLogout = new JPanel();
        panelLogout.setPreferredSize(new Dimension(20, 20));
        panelLogout.setBorder(BorderFactory.createTitledBorder("LOGOUT: "));
        TExtFieldUserLogout = new JTextField(10);

        //utilizzo una classe anonima per poter implementare un actionPerformed in modo da consentire all utente di usare il tasto invio
        //per eseguire il tasto Logout
        TExtFieldUserLogout.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) {log.doClick();}});

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
        TextFieldWordSendWord.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) {log.doClick();}});

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
    private void StopCaptureUDPMessages() {


        //qui per far sbloccare dalla receive il thread devo inviare un packet in cui inserisco
        //la stringa logout, anche gli altri thread degli altri client riceveranno tale stringa
        //che verrà ignorata, quest ultimo invece riceverà anche una interrupt

        try (DatagramSocket sckStopThread = new DatagramSocket()){

            byte [] data = new String("logout").getBytes();
            DatagramPacket pkj = new DatagramPacket(data, 0, data.length, addressMulticat);

            multiCastThread.interrupt();//lancio un interruzione che verrà testata nella guardia del while
            sckStopThread.send(pkj);//invio il messaggio di terminazione per far sbloccare il thread dalla receive

            sockMulticast.leaveGroup(addressMulticat, null);//esco dal gruppo multicast

        }
        catch (Exception e){e.printStackTrace();}

    }
    private byte [] ReadDataByte(DataInputStream inn) {

        byte [] data = null;
        try {
            int read = 0, len = inn.readInt();

            data = new byte[len];
            while(read < len) {
                data[read] = inn.readByte();
                read++;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return data;
    }
}
