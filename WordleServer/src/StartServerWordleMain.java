//Classe che si occupa delle diverse operazioni di configurazione, si occupa di effettuare la
//serializzazione/deserializzazione dell'oggetto WordleServer in modo da salvare e recuperare lo stato del
//server ad ogni riavvio del server

/**
 * Appunti miei che devo copiare nel mio file, ora per prima cosa devo instanziare il server e
 * implementare la registrazione tramite RMI, per ora ho implementato la struttura dati utente e gioco, entrambe implementano serializable
 */
public class StartServerWordleMain {

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
         */
        try {

            ServerWordle server = new ServerWordle();
            Thread.sleep(30000);//dormo per 30 secondi e poi chiudo
                                      //il servizio rmi e quindi anche il server per ora
            server.ShutDownRMI();
        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }
}
