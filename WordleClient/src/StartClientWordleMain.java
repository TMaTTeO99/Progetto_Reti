import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

            /**
             * Faccio un shell interattiva rudimentale solo per cominciare a provare le funzionalita piano piano
             */
            ImplementazioneNotificaClient notifica = null;
            Registrazione servizio = null;
            String user = null;
            String pass = null;
            Scanner in = new Scanner(System.in);
            int scelta = 0;
            while(scelta != -1){
                System.out.println("INSERIRE OPZIONE DESIDERATA: ");
                System.out.println("1 per effettuare registrazione");
                System.out.println("2 per effettuare login");
                System.out.println("-1 per uscire");
                scelta = in.nextInt();
                if(scelta == 1){
                    servizio = (Registrazione)LocateRegistry.getRegistry(portRMI).lookup("Registrazione");
                    user = in.next();
                    pass = in.next();
                    System.out.println(servizio.registra(user, pass));
                }
                else if(scelta == 2) {

                    user = in.next();
                    pass = in.next();
                    servizio = (Registrazione)LocateRegistry.getRegistry(portRMI).lookup("Registrazione");

                    //test per connettermi al server e vedere cosa fa
                    Socket sck = new Socket();
                    try {//1025 porta corretta per questo client
                        sck.connect(new InetSocketAddress("localhost", 6501));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    byte [] tmp = new byte[100];
                    ByteArrayInputStream BuffIn = new ByteArrayInputStream(tmp);
                    try (DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(sck.getOutputStream()));
                         DataInputStream inn = new DataInputStream(new BufferedInputStream(sck.getInputStream()))) {

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
                                notifica = new ImplementazioneNotificaClient();//oggetto da esportare
                                NotificaClient skeleton = (NotificaClient) UnicastRemoteObject.exportObject(notifica, 0);
                                /**
                                 * A questo punto il client non termina perhce rimane esportato l'ggetto
                                 * per effettuare la notifica, naturalmente dovra essere esportato quando
                                 * il client fara la login e eliminata l'esportazione in seguito a una logout
                                 */
                                //ora qua provo a inviare lo stub al server dopo che mi sono registrato ecc
                                servizio.sendstub(user, skeleton);
                                break;
                            case -1:
                                System.out.println("Errore. Per partecipare al gioco bisogna prima essere iscritti");
                                break;
                            case -2:
                                System.out.println("Errore. Password inserita non corretta");
                                break;
                        }
                    }
                    catch (Exception e ) {e.printStackTrace();}

                }
                else {
                    UnicastRemoteObject.unexportObject(notifica, true);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

}
