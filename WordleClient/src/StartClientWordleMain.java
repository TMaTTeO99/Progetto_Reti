import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class StartClientWordleMain {


    /**
     * Per ora è solo un client di prova per vedere se lato server avviene correttamente la registrazione
     * -------------------------------------
     * Comincio con l'implementazione del servizio di notifica offerto dal server, devo quindi costruire le interfacce lato
     * client e lato server
     */
    private static final int portRMI = 6500;
    public static void main(String [] args) {

        try {
            Scanner in = new Scanner(System.in);
            ImplementazioneNotificaClient notifica = new ImplementazioneNotificaClient();//oggetto da esportare
            NotificaClient skeleton = (NotificaClient) UnicastRemoteObject.exportObject(notifica, 0);

            Registrazione servizio = (Registrazione)LocateRegistry.getRegistry(portRMI).lookup("Registrazione");
            System.out.println(servizio.registra(in.next(), in.next(), skeleton));
            /**
             * A questo punto il client non termina perhce rimane esportato l'ggetto
             * per effettuare la notifica, naturalmente dovra essere esportato quando
             * il client fara la login e eliminata l'esportazione in seguito a una logout
             */
        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }

}
