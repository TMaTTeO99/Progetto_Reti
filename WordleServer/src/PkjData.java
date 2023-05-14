import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

//classe utilizzata per contenere i dati da inviare al client riguardo la richiesta ricevuta e per poter gestire la nonblocking mode
public class PkjData {


    private ByteBuffer RequestBuff;//ByteBuffer per leggere dal channel e scrivere dentro Request
    private byte [] Request;//variabile usata per salvare la richiesta che viene effettuata finche non è possibile soddisfarla
    private int IdxRequest = 0;//indice all di dove sono arrivato a scrivere dentro Request
    private byte [] Answer;//Variabile in cui memorizzare la risposta al client
    private int IdxAnswer = 0;//indice all di dove sono arrivato a scrivere dentro Answer
    /**
     * Bisogna completare la classe con le stutture ddti necessarie per contenere i dati e i vari metodo per accedervi
     * Per ora provo solo getCompleted/setCompleted per testare la nonblockingmode
     */
    public void allocRequest(int ln) {Request = new byte[ln];}
    public byte [] getRequest() {return Request;}
    public void setIdxRequest(int idx) {IdxRequest = idx;}
    public int getIdxRequest() {return IdxRequest;}
    public int getIdxAnswer() {return IdxAnswer;}
    public void allocAnswer(int ln) {Answer = new byte[ln];}
    public byte [] getAnswer() {return Answer;}
    public void SetAnswer(byte [] ByteAnswer) {
        Answer = ByteAnswer;
        IdxAnswer = Answer.length;
    }
}
