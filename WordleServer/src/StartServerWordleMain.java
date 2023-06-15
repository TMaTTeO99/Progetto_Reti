//Classe che si occupa delle diverse operazioni di configurazione, si occupa di effettuare la
//serializzazione/deserializzazione dell'oggetto WordleServer in modo da salvare e recuperare lo stato del
//server ad ogni riavvio del server

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

/**
 * Appunti miei che devo copiare nel mio file, ora per prima cosa devo instanziare il server e
 * implementare la registrazione tramite RMI, per ora ho implementato la struttura dati utente e gioco, entrambe implementano serializable
 */
public class StartServerWordleMain {

    private static final String PathStart = "../"; //Path della dir da cui cominciare la ricerca del file

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
        GetDataConfig ConfigureData = new GetDataConfig("configServer.txt", "../");

        //Ricerca e apertura del file di configurazione
        ConfigureData.SearchFile(new File(ConfigureData.getPathStart()));

        //Chiusura in caso di file di configurazione non trovato
        if(ConfigureData.getConfigureFile() == null){
            System.out.println("ERRORE. File di configurazione assente");
            return;
        }

        //a questo punto devo effettuare il pars del file
        try {ConfigureData.ReadConfig();}
        catch (Exception e) {e.printStackTrace();}

        //a questo punto ho recuperato le prima info di configurazione
        //tali info le passo al server per l'elaborazione

        //prima di istanziare il server leggo il vocabolario e lo inserisco in una struttura dati opportuna
        ArrayList<String> vocabolario = ConfigureData.getVocabolario();
        try {

            ServerWordle server = new ServerWordle(vocabolario, ConfigureData);
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
