import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

//classe utilizzata per contenere i dati da inviare al client riguardo la richiesta ricevuta e per poter gestire la nonblocking mode
public class Attached {

    private ReentrantLock lock = new ReentrantLock();//lock per gestire l accesso all'attached fra piu thread
    private boolean Completed = false;//indicherà se i dati presenti sono completi oppure no (Inizialmente no)
    private boolean StartRead = false;//variabile che indica se si è gia iniziato a leggere i dati oppure no
    private ByteBuffer RequestBuff;//ByteBuffer per leggere dal channel e scrivere dentro Request
    private byte [] Request;//variabile usata per salvare la richiesta che viene effettuata finche non è possibile soddisfarla
    private int IdxRequest = 0;//indice all di dove sono arrivato a scrivere dentro Request
    private byte [] Answer;//Variabile in cui memorizzare la risposta al client
    private int IdxAnswer = 0;
    /**
     * Bisogna completare la classe con le stutture ddti necessarie per contenere i dati e i vari metodo per accedervi
     * Per ora provo solo getCompleted/setCompleted per testare la nonblockingmode
     */
    public void allocRequest(int ln) {Request = new byte[ln];}
    public byte [] getRequest() {return Request;}
    public void setIdxRequest(int idx) {IdxRequest = idx;}
    public int getIdxRequest() {return IdxRequest;}
    public boolean isCompleted() {return Completed;}
    public void setCompleted(boolean Status) {Completed = Status;}
    public boolean isStartRead() {return StartRead;}
    public void setStartRead(boolean startRead) {StartRead = startRead;}
    public ReentrantLock getLock() {return lock;}
    public void WriteRequest(SocketChannel channel) {

        RequestBuff = ByteBuffer.wrap(Request);
        try {setIdxRequest(channel.read(RequestBuff));}//leggo dal channel e setto l'indice IdxRequest di attached
        catch (Exception e) {e.printStackTrace();}

    }
}
