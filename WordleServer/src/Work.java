import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Work implements Runnable{

    private static final int SIZE_SIZE = 4;
    private SelectionKey Key;//variabile utilizzata per reperire e settare i dati per le comunicazioni come ad es il channell e l attach
    private ReentrantReadWriteLock RWlck;
    private Selector Selettore;
    private Lock write;
    public Work(SelectionKey k, ReentrantReadWriteLock lock, Selector s) {
        Key = k;
        RWlck = lock;
        Selettore = s;
    }
    public void run() {

        //per implementare la soluzione non bloccante devo per prima cosa recuperare l attached e il channel
        SocketChannel channel = (SocketChannel) Key.channel();
        Attached dati = (Attached)Key.attachment();
        try {

            dati.getLock().lock();//acquisisco la mutua esclusione sulle variabili dell attached

            if(!dati.isStartRead()){//caso in cui non ho iniziato a leggere i dati che il client ha inviato

                dati.setStartRead(true);//setto StartRead cosi la prossima volta che comincio a leggere entro nell ramo else

                //array di 4 byte per recuperare la dim del messaggio ricevuto
                byte [] LenMexByte = new byte[SIZE_SIZE];
                ByteBuffer LenMexBuffer = ByteBuffer.wrap(LenMexByte);

                channel.read(LenMexBuffer);//leggo la len della richiesta
                LenMexBuffer.flip();//resetto position per poter leggere dal ByteBuffer
                dati.allocRequest(LenMexBuffer.getInt());//alloco il vettore che conterrà la richiesta dentro attached

                dati.WriteRequest(channel);
                channel.register(Selettore, SelectionKey.OP_READ, dati);

            }
            else {//caso in cui ho cominciato gia a leggere dei dati e devo continuare

                //a questo punto devo controllare prima se ho finito i dati da leggere (quelli che il client ha inviato)
                if(dati.getIdxRequest() != dati.getRequest().length) {//caso in cui non ho letto tutta la request

                    //a questo punto quindi devo continuare a leggere da dovee avevo ripreso che è inidcato da dati.getIdxRequest()
                    dati.WriteRequest(channel);
                    channel.register(Selettore, SelectionKey.OP_READ, dati);
                }
                else {//caso in cui ho letto tutta la request => devo preparare la risposta e fare le diverse operazioni

                    //ora provo a testare
                    System.out.println(new String(dati.getRequest(), StandardCharsets.UTF_16) + " qui");
                    /**
                     * A questo punto devo leggere i dati ricevuti e in base alla richiesta fare le diverse operazioni,
                     * dopo di che posso registrare il channel per la scrittura per dare la risposta al client
                     */


                    //channel.register(Selettore, SelectionKey.OP_WRITE, dati);

                }
            }
        }
        catch (Exception e) {e.printStackTrace();}
        finally {
            dati.getLock().unlock();//rilascio la mutua esclusione sulle variabili dell attached
        }

    }
}
