import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingDeque;

//classe che viene utilizzata perr stare in ascolto dei messaggi udp che il server invia sul gruppo multicast
public class CaptureUDPmessages implements Runnable{

    private final int MAX_SIZE = 256;//utilizzo una dimensione per datagrampacket di 256 byte
    private InetSocketAddress address;
    private LinkedBlockingDeque<Suggerimenti> SuggerimentiQueue;
    public CaptureUDPmessages(String IP_multicast, int port, LinkedBlockingDeque<Suggerimenti> SuggQueue) throws Exception{
        address = new InetSocketAddress(IP_multicast, port);
        SuggerimentiQueue = SuggQueue;
    }

    public void run() {


        try (MulticastSocket sock = new MulticastSocket(address)){

            sock.joinGroup(address, null);//non specifico nessuna interfaccia di rete per essere piu generico possibile

            while(!Thread.interrupted()) {//finche il thread non riceve un interruzione continua a ciclare

                byte [] datiDaricevere = new byte[MAX_SIZE];//byte array in cui verranno inseriti i dati dedl packet

                DatagramPacket packet = new DatagramPacket(datiDaricevere, 0, MAX_SIZE);
                sock.receive(packet);//recupero il packet

                //leggo i dati dal paket tramite datainputstream
                try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData()))) {

                    byte [] dati = new byte[packet.getLength()];//byte array di supporto
                    in.readNBytes(dati, 0, dati.length);//recupero il numero corretto di byte per poter trasformare i dati in stringa
                    String tentativiUtente = new String(dati, StandardCharsets.UTF_8);

                    //Ora faccio solo dei test per vedere lato server e lato client se funzionano bene:
                    System.out.println(tentativiUtente + " tentativi ricevuti");

                    //qui ora devo usare un tokenizzatore per poter recuperare ogni tentativo, alla fine della lista dei tentativi
                    //verra inserito il nome dell utente
                    Suggerimenti sugg = new Suggerimenti();
                    StringTokenizer tok = new StringTokenizer(tentativiUtente, " ");

                    sugg.setUtente(tok.nextToken());
                    while(tok.hasMoreTokens()) {sugg.addSuggerimento(tok.nextToken());}

                    SuggerimentiQueue.put(sugg);

                }
                catch (Exception e) {e.printStackTrace();}

            }
        }
        catch (Exception e) {e.printStackTrace();}
    }
}
