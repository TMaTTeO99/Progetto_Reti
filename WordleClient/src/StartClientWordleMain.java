import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

public class StartClientWordleMain {

    private static String SecurityKey; // chiave per la cifratura
    private static int ID_Channel = -1;// id che il server assegna alla connessione quando il client si collega la prima volta

    public static void main(String [] args) {

        try {

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

            Socket socket = new Socket();//creo l oggetto socket e mi connetto al server appena avvio
            try {socket.connect(new InetSocketAddress(dataConfig.getIP_server(), dataConfig.getPort_ListeningSocket()));}
            catch (Exception e) {e.printStackTrace();}

            //prima di avviare la GUI costruisco insieme al server la chiave di sicurezza per la sessione
            if(!SendAndRicevereSecurityData(socket, dataConfig)) {

                //In caso di fallimento della costruzione della chiave di sessione chiudo tutto
                System.out.println("Errore. Impossibile costruire chiave di sessione");
                socket.close();
                return;

            }
            System.out.println(SecurityKey);
            StartLoginRegistrazione game = new StartLoginRegistrazione(dataConfig, SuggQueue, ID_Channel, SecurityKey, socket);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static boolean SendAndRicevereSecurityData(Socket socket, GetDataConfig dataConfig) {

        int flag = 0, lendata = 0;
        BigInteger c = null;//segreto del client in questo caso
        BigInteger P = new BigInteger(String.valueOf(dataConfig.getP()));//Intero P come biginteger
        String nameMethod = "dataforkey:";//metodo che deve eseguire il server

        //calcolo C per il protocollo DH
        BigInteger C = SecurityClass.Compute_C(dataConfig.getG(), dataConfig.getP());//calcolo il segreto del client e poi g^segreto mod P
        BigInteger S = null;//dati che ricevo dal server

        try{
            //comunico con il server
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            ou.writeInt(((("dataforkey:"+ C.toString()).length()) * 2));
            ou.writeChars("dataforkey:"+C.toString());
            ou.flush();
            in.readInt(); //scarto la len del messaggio
            ID_Channel = in.readInt();//recupero l'id da usare per la registrazione

            flag = in.readInt();//recupero l intero che indica se è andato tutto bene
            if(flag != 0) {

                String key = ReadData(in);
                if(key != null) {S = new BigInteger(key);}//recuporo i dati

            }
            else return false;//caso di errore
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        try {c = SecurityClass.getSecret();}
        catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        SecurityKey = S.modPow(c, P).toString(2);//calcolo la chiave di sessione come S^segreto mod P
        while(SecurityKey.length() < 16){SecurityKey += '0';}//se la chiave è < 128 bit faccio pudding

        return true;
    }
    private static boolean IsGenerator(Integer g, Integer p) {

        for (int i = 1; i < p - 1; i++) {
            int result = (int) Math.pow(g, i) % p;
            if (result == 1) {return false;}
        }
        return true;
    }
    private static boolean CheckPG(Integer p, Integer g) {

        BigInteger Pbig = new BigInteger(String.valueOf(p));
        if(Pbig.isProbablePrime(30)) {//uso una precisione di 10 in modo che P sia primo con probabilità 1 - (1/4)^10
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
    private static String ReadData(DataInputStream inn) {

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
}
