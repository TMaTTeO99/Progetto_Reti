import java.io.*;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.util.ArrayList;


public class StartServerWordleMain {

    private static final String PathStart = ".." + FileSystems.getDefault().getSeparator(); //Path della dir da cui cominciare la ricerca del file di config

    //metodo usato per fare il controllo del parametro indicato come generatore di Zp
    private static boolean IsGenerator(Integer g, Integer p) {

        for (int i = 1; i < p - 1; i++) {
            int result = (int) Math.pow(g, i) % p;
            if (result == 1) {return false;}
        }
        return true;
    }
    //metodo usato per fare il controllo del parametro P usato come numero primo per creare la chiave di sucurezza
    private static boolean CheckPG(Integer p, Integer g) {

        BigInteger Pbig = new BigInteger(String.valueOf(p));
        if(Pbig.isProbablePrime(30)) {//uso una precisione di 10 in modo che P sia primo con probabilità 1 - (1/4)^10
            return IsGenerator(g, p);
        }
        return false;
    }
    //in caso di errore nel file di config viene usato questo metodo per settare dei parametri di default
    private static void SetDefaultData(GetDataConfig data) {

        data.setIP_Multicast("239.0.0.1");
        data.setURLtranslate("https://api.mymemory.translated.net/");
        data.setLastTimeWord(0);
        data.setMaxThread(5);
        data.setTimeStempWord(86400000);
        data.setPathSerialization(".." + FileSystems.getDefault().getSeparator() + "JsonSerialization");
        data.setPortExport(6500) ;
        data.setPort_Multicast(5240);
        data.setPort_ListeningSocket(6501);
        data.setIP_server("localhost");
        data.setPathVocabolario(".." + FileSystems.getDefault().getSeparator() + "vocabolario.txt");
        data.setG(2);
        data.setP(13);
        data.setAfterUpDate(1);

    }
    public static void main(String [] args) {

        ArrayList<String> vocabolario = null;
        GetDataConfig ConfigureData = new GetDataConfig("configServer.txt", PathStart);

        //Ricerca e apertura del file di configurazione
        ConfigureData.SearchFile(new File(ConfigureData.getPathStart()));

        //Chiusura in caso di file di configurazione non trovato
        if(ConfigureData.getConfigureFile() == null){
            System.out.println("ERRORE. File di configurazione assente");
            return;
        }

        //a questo punto devo effettuare il pars del file
        try {ConfigureData.ReadConfig();}
        catch (Exception e) {
            System.out.println("ERRORE Nel file di configurazione");
            System.out.println("Verrannno utilizzati i parametri di default per configurare il server");

            //in questo caso aggiungo all oggetto ConfigureData i dati di default per il server
            SetDefaultData(ConfigureData);
        }
        if(!CheckPG(ConfigureData.getP(), ConfigureData.getG())) {//in caso non siano corretti uso i vaoli di default
            ConfigureData.setG(2);
            ConfigureData.setP(13);
        }

        //prima di istanziare il server leggo il vocabolario e lo inserisco in una struttura dati opportuna
        try {vocabolario = ConfigureData.getVocabolario();}
        catch (Exception e) {
            System.out.println("Vocabolario non trovato");
            return;
        }
        try {
            ServerWordle server = new ServerWordle(vocabolario, ConfigureData);
            server.StartServer();
        }
        catch (Exception ignored) {}
        System.out.println("Chiusura Server. Probabile errore sulle porte utilizzate");
    }
}
