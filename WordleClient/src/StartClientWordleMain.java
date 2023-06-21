import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;

public class StartClientWordleMain {

    /**
     * Comincio con l implementazione corretta del client in modo da poter testare correttamente il server
     *
     */
    private static boolean IsGenerator(Integer g, Integer p) {

        for (int i = 1; i < p - 1; i++) {
            int result = (int) Math.pow(g, i) % p;
            if (result == 1) {return false;}
        }
        return true;
    }
    private static boolean CheckPG(Integer p, Integer g) {

        BigInteger Pbig = new BigInteger(String.valueOf(p));
        if(Pbig.isProbablePrime(10)) {//uso una precisione di 10 in modo che P sia primo con probabilità 1 - (1/4)^10
            return IsGenerator(g, p);
        }
        return false;
    }
    private static void SetDefaultData(GetDataConfig data) {

        data.setIP_Multicast("239.0.0.1");
        data.setPortExport(6500) ;
        data.setPort_Multicast(5240);
        data.setPort_ListeningSocket(6501);
        data.setIP_server("localhost");
        data.setG(2);
        data.setP(13);

    }
   // private static final int portRMI = 6500;
    public static void main(String [] args) {


        try {

            /**
             *  A questo punto, non elimino questa parte gia scritta ma la commento, pprossimamente faro in modo di poter lanciare
             *  il client in 2 mod con la gui e senza in modo da poter testare l app correttamente
             *
             */
            GetDataConfig dataConfig = new GetDataConfig("configClient.txt", "../");

            dataConfig.SearchFile(new File(dataConfig.getPathStart()));
            if(dataConfig.getConfigureFile() == null) {
                System.out.println("ERRORE. File di configurazione assente");
                return;
            }
            //a questo punto devo effettuare il pars del file
            try {dataConfig.ReadConfig();}
            catch (Exception e) {
                System.out.println("ERRORE Nel file di configurazione");
                System.out.println("Verrannno utilizzati i parametri di default per configurare il client");

                //in questo caso aggiungo all oggetto ConfigureData i dati di default per il server
                SetDefaultData(dataConfig);
            }
            ArrayList<Suggerimenti> SuggQueue = new ArrayList<>();//struttura dati per memorizzare le condivisioni degli utenti


              //controllo se P e G recuperati dal file di config sono corretti per lo scambio della chiave si sessione
            if(!CheckPG(dataConfig.getP(), dataConfig.getG())) {//in caso non siano corretti uso i vaoli di default
               dataConfig.setG(2);
               dataConfig.setP(13);
            }
            StartLoginRegistrazione game = new StartLoginRegistrazione(dataConfig, SuggQueue);
            /*
            ImplementazioneNotificaClient notifica = null;
            Registrazione servizio = null;
            String user = null;
            String pass = null;
            Scanner in = new Scanner(System.in);
            Socket sck = null;
            int login = 0;
            int scelta = 0;
            while(scelta != -1){
                System.out.println("INSERIRE OPZIONE DESIDERATA: ");
                System.out.println("1 per effettuare registrazione");
                System.out.println("2 per effettuare login");
                System.out.println("3 per effettuare logout");
                System.out.println("-1 per uscire");
                scelta = in.nextInt();
                if(scelta == 1){
                    //gia implementata nella gui
                    servizio = (Registrazione)LocateRegistry.getRegistry(portRMI).lookup("Registrazione");
                    user = in.next();
                    pass = in.next();
                    if(servizio.registra(user, pass) == 0) System.out.println("Username gia utilizzato");
                    else System.out.println("Registrazione completata");
                }
                else if(scelta == 2) {

                    if(login != 1) {
                        login = 1;
                        user = in.next();
                        pass = in.next();
                        servizio = (Registrazione)LocateRegistry.getRegistry(portRMI).lookup("Registrazione");

                        //test per connettermi al server e vedere cosa fa
                        sck = new Socket();
                        try {//1025 porta corretta per questo client
                            sck.connect(new InetSocketAddress("localhost", 6501));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        byte [] tmp = new byte[100];
                        ByteArrayInputStream BuffIn = new ByteArrayInputStream(tmp);
                        //try () {
                        DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(sck.getOutputStream()));
                        DataInputStream inn = new DataInputStream(new BufferedInputStream(sck.getInputStream()));
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

                        switch(inn.readInt()) {
                            case 0 :
                                System.out.println("Login effettuato");
                                System.out.println("Benvenuto");
                                //notifica = new ImplementazioneNotificaClient();//oggetto da esportare
                                //NotificaClient skeleton = (NotificaClient) UnicastRemoteObject.exportObject(notifica, 0);
                                /**
                                 * A questo punto il client non termina perhce rimane esportato l'ggetto
                                 * per effettuare la notifica, naturalmente dovra essere esportato quando
                                 * il client fara la login e eliminata l'esportazione in seguito a una logout
                                 */
                                //ora qua provo a inviare lo stub al server dopo che mi sono registrato ecc
                               // servizio.RegisryForCallBack(user, skeleton);
            /*
                                break;
                            case -1:
                                System.out.println("Errore. Per partecipare al gioco bisogna prima essere iscritti");
                                break;
                            case -2:
                                System.out.println("Errore. Password inserita non corretta");
                                break;
                        }
                    }
                    else System.out.println("Hai gia effettuato il login");

                }
                else if(scelta == 3) {
                    if(sck != null) {
                        login = 0;
                        user = in.next();
                        byte [] tmp = new byte[100];
                        ByteArrayInputStream BuffIn = new ByteArrayInputStream(tmp);
                        DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(sck.getOutputStream()));
                        DataInputStream inn = new DataInputStream(new BufferedInputStream(sck.getInputStream()));

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
                                    System.out.println("Logout effettuato");
                                    break;
                                case -1:
                                    System.out.println("Errore. Per effettuare il logout bisogna prima essere iscritti");
                                    break;
                                case -2:
                                    System.out.println("Errore. Per effettuare il logout bisogna prima aver effettuato il logout");
                                    break;
                                case -3:
                                    System.out.println("Errore. Username inserito non corretto");
                                    break;
                            }
                    }
                    else System.out.println("Errore, Impossibile effettuare il logout se prima non si è effettuato il login");
                }
                else {
                    if(notifica != null) UnicastRemoteObject.unexportObject(notifica, true);
                }
            }

             */
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
