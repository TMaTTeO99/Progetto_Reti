import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Work implements Runnable{

    private SelectionKey Key;//variabile utilizzata per reperire e settare i dati per le comunicazioni come ad es il channell e l attach
    private ReentrantReadWriteLock RWlck;
    private Lock write;
    public Work(SelectionKey k, ReentrantReadWriteLock lock) {
        Key = k;
        RWlck = lock;
    }
    public void run() {

        //Ora devo recuperare il channell dalla key
        SocketChannel channel = (SocketChannel) Key.channel();

        //adesso qui devo leggere i dari che ricevo sul channel




    }
}
