import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//classe che viene utilizzata perr stare in ascolto dei messaggi udp che il server invia sul gruppo multicast
public class CaptureUDPmessages implements Runnable{

    private final int MAX_SIZE = 256;//utilizzo una dimensione per datagrampacket di 256 byte
    private Lock lock;
    private ArrayList<Suggerimenti> SuggerimentiQueue;
    private MulticastSocket socket;
    public CaptureUDPmessages(MulticastSocket sckM, ArrayList<Suggerimenti> SuggQueue, ReentrantLock lck) throws Exception{

        SuggerimentiQueue = SuggQueue;
        lock = lck;
        socket = sckM;
    }

    public void run() {


        try {

            while(!Thread.interrupted()) {//finche il thread non riceve un interruzione continua a ciclare

                byte [] datiDaricevere = new byte[MAX_SIZE];//byte array in cui verranno inseriti i dati dedl packet

                DatagramPacket packet = new DatagramPacket(datiDaricevere, 0, MAX_SIZE);
                socket.receive(packet);//recupero il packet
                System.out.print("PACKETTO RICEVUTO");
                //leggo i dati dal paket tramite datainputstream
                try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData()))) {

                    byte [] dati = new byte[packet.getLength()];//byte array di supporto
                    in.readNBytes(dati, 0, dati.length);//recupero il numero corretto di byte per poter trasformare i dati in stringa
                    String tentativiUtente = new String(dati, StandardCharsets.UTF_8);

                    //Ora faccio solo dei test per vedere lato server e lato client se funzionano bene:
                    System.out.println(tentativiUtente + " tentativi ricevuti");
                    if(!tentativiUtente.equals("logout")) {

                        //qui ora controllo se ho ricevuto i dati dal server o è il client stesso che sta facendo il logout.
                        //controllo quindi che dati ho ricevuto,

                        //qui ora devo usare un tokenizzatore per poter recuperare ogni tentativo
                        //all inizio della stringa inviata dal server è presente il nome dell utente

                        Suggerimenti sugg = new Suggerimenti();
                        StringTokenizer tok = new StringTokenizer(tentativiUtente, " ");

                        sugg.setUtente(tok.nextToken());//recupero il nome dell utente
                        while(tok.hasMoreTokens()) {sugg.addSuggerimento(tok.nextToken());}

                        lock.lock();
                        SuggerimentiQueue.add(sugg);
                        lock.unlock();
                    }
                    else System.out.println("Chido ricezione condivisioni");
                }
                catch (Exception e) {e.printStackTrace();}
            }
        }
        catch (Exception e) {e.printStackTrace();}
        System.out.println("ESCO DAL RUN :)");
    }
}
