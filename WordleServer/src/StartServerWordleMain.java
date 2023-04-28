//Classe che si occupa delle diverse operazioni di configurazione, si occupa di effettuare la
//serializzazione/deserializzazione dell'oggetto WordleServer in modo da salvare e recuperare lo stato del
//server ad ogni riavvio del server

import java.io.*;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

/**
 * Appunti miei che devo copiare nel mio file, ora per prima cosa devo instanziare il server e
 * implementare la registrazione tramite RMI, per ora ho implementato la struttura dati utente e gioco, entrambe implementano serializable
 */
public class StartServerWordleMain {

    private static final long DayInMS = 86400000;//conversione di un giorno in millisecondi
    private static final long OraInMS = 3600000; //conversione di 24 ore in millisecondi
    private static int MaxThread = 5;//di default assegno un numero di thread cosi in caso di errore nel file di config
                                     //iol server comunque puo lavorare
    private static long TimeStempWord = DayInMS;
    private static String ConfigureFileName = "config.txt"; //nome del file contenente le info di configurazione
    private static String PathSerialization;
    /**
     * WORNIGGGGGGG!!!!!!!!:
     * tale path dovra essere modificato prima della consegna in base a come verranno messe le cartelle
     * nella dir finale, per ora è solo per me
     * */
    private static final String PathStart = "../"; //Path della dir da cui cominciare la ricerca del file

    private static File ConfigureFile = null;

    //metodo privato per convertire il tempo indicato all'interno del file di config in millisecondi
    private static long ToMLS(String arg) {

        //il formato della stringa arg è: giorni ore
        long tm = 0;

        StringTokenizer tok = new StringTokenizer(arg, " ");
        tm += DayInMS * Integer.parseInt(tok.nextToken());
        tm += OraInMS * Integer.parseInt(tok.nextToken());

        return tm;
    }
    //metodo privato per effettuare il pars di ogni linea letta dal file di configurazione
    private static void ParsCLine(String ln) {

        StringTokenizer tok = new StringTokenizer(ln, ":");
        while(tok.hasMoreTokens()) {
            try {
                switch(tok.nextToken()) {
                    case "thread" :
                        MaxThread = Integer.parseInt(tok.nextToken());
                        break;
                    case "timeword" :
                        TimeStempWord = ToMLS(tok.nextToken());//metodo privato per la conversione in long
                        break;
                    case "pathjson" :
                        PathSerialization = tok.nextToken();
                        break;
                }
            }
            catch (Exception e) {
                System.out.println("ERRORE Nel file di configurazione");
                System.out.println("Verrannno utilizzati i parametri di default per configurare il server");
            }
        }
    }
    private static void ReadConfig(BufferedReader inConfig) throws Exception{

        String line = null;
        while((line = inConfig.readLine()) != null) {ParsCLine(line);}
    }
    //metodo statico per la ricerca del path del file di configurazione
    public static void SearchFile(File curfile) {

        String [] lst = curfile.list();
        File next = null;
        for(int i = 0; i<lst.length; i++) {
            next = new File(curfile+"/"+lst[i]);
            try {
                if(next.isFile()) {
                    if(next.getName().equals(ConfigureFileName))ConfigureFile = new File(next.getPath());
                }
                else if(next.isDirectory()) SearchFile(next);
            }
            catch (Exception e) {e.printStackTrace();}
        }
    }
    public static void main(String [] args) {

        /**
         * A questo punto nel main dovro configurare il server
         * numeri di porta, indirizzi, valori di timeout, vocabolario, numero di thread che saranno usati dal server,
         * voglio implementare un numero limitato di worker in modo da avere un basso numero di context switch
         *
         *
         *
         * Riguardo il periodo che intercorre fra due publicazioni successive di una parole, all interno del file
         * cerco di inserire un timestamp abbastanza semplice da elaborare, il gioco originale publica una parola ogni giorno
         * qui invece posso decire ogni quanto tempo viene publicata la nuova parola, nel file inserisco:
         * TimeWord: D H     ( d >= 0, H > 0)
         *
         * Per ora creo il file solo per vedere il TimeWord e per poter implementare la regiostrazione e la serializzazione
         * Comincio la fase di configurazione:
         */
        //Ricerca e apertura del file di configurazione
        SearchFile(new File(PathStart));

        //Chiusura in caso di file di configurazione non trovato
        if(ConfigureFile == null){
            System.out.println("ERRORE. File di configurazione assente");
            return;
        }
        System.out.println(ConfigureFile.getPath());//lascio questa stampa per eventualmente ritrovare il file
        //a questo punto devo effettuare il pars del file
        try(BufferedReader inConfig = new BufferedReader(new FileReader(ConfigureFile))) {
            ReadConfig(inConfig);
        }
        catch (Exception e) {e.printStackTrace();}

        //a questo punto ho recuperato le prima info di configurazione
        //tali info le passo al server per l'elaborazione
        System.out.println(PathSerialization);
        try {

            ServerWordle server = new ServerWordle(PathSerialization, MaxThread, TimeStempWord);
            //Thread.sleep(20000);//dormo per 30 secondi e poi chiudo
            //il servizio rmi e quindi anche il server per ora

            server.StartServer();
            //server.ShutDownRMI();
        }
        catch (Exception e) {
            if(e instanceof SecurityException) {System.out.println("Fallita Creazione Della Directory Per I File Json");}
            e.printStackTrace();
        }


    }
}
