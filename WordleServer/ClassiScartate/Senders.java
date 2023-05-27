import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.Future;

public class Senders implements Runnable {


    private SelectionKey key;
    private HashMap<Integer, KeyData> LstDati;
    public Senders(SelectionKey k, HashMap<Integer, KeyData> lst) {
        key = k;
        LstDati = lst;
    }
    public void run() {

        SocketChannel channel = (SocketChannel) key.channel();//recupero il canale
        Future<PkjData> DataFuture = LstDati.get((Integer) key.attachment()).getDati();

        try {//provo a scrivere i dati

            PkjData dati = DataFuture.get();
            int n = 0;
            while(n != dati.getIdxAnswer())n += channel.write(ByteBuffer.wrap(dati.getAnswer()));
        }
        catch (Exception e) {//se viene sollevata un eccezione perche il client ha chiuso la connessione elimino il channel selettore
            key.cancel();
        }
    }
}
