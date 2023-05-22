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

    private String usname;//variabile usata per poter accedere alla struttura dati dei registrati
                          //e modificare il campo login a false
                          // quando il client chiude la connessione senza effettuare il logout



    //metodi set e get
    public String getUsname() {return usname;}
    public void setUsname(String s) {usname = s;}
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
