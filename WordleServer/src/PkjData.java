import java.nio.ByteBuffer;

//classe utilizzata per contenere i dati da inviare al client riguardo la richiesta ricevuta e per poter gestire la nonblocking mode
public class PkjData {


    private ByteBuffer RequestBuff;//bytebuffer che conterra la richiesta del client
    private byte [] Request;//array di supporto per RequestBuff
    private byte [] Answer;//array che conterrà i byte della risposta
    private int IdxAnswer = 0;//indice all di dove sono arrivato a scrivere dentro Answer

    private byte [] lenMex = new byte[4];//byte array di supporto per la len del messaggio
    private ByteBuffer LenMexBuffer = ByteBuffer.wrap(lenMex);//bytebuffer usato per recuperare la len della richiesta del client

    public void allocRequest(int ln) {
        Request = new byte[ln];
        RequestBuff = ByteBuffer.wrap(Request);
    }
    public void SetAnswer(byte [] ByteAnswer) {
        Answer = ByteAnswer;
        IdxAnswer = Answer.length;
    }
    public byte [] getRequest() {return Request;}
    public int getIdxAnswer() {return IdxAnswer;}
    public void allocAnswer(int ln) {Answer = new byte[ln];}
    public byte [] getAnswer() {return Answer;}

    public ByteBuffer getLenMexBuffer() {return LenMexBuffer;}

    public ByteBuffer getRequestBuff() {return RequestBuff;}
}
