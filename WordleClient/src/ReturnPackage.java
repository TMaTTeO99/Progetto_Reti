import java.io.DataInputStream;

//classe usata per poter far si che l eventuale doInBackground method possa inviare i vaolri coprretti all eventuale metodo done
public class ReturnPackage {

    private int ReturnValue;
    private DataInputStream inn;//Datainputstream usato per poter continuare a leggere i dati che il server invia dopo aver letto
                                //l intero che indica la riuscita o il fallimento di una operazione

    public ReturnPackage(int RetVal, DataInputStream i) {
        ReturnValue = RetVal;
        inn = i;
    }
    public ReturnPackage(int RetVal) {
        ReturnValue = RetVal;
    }

    //metodi get per recuperare i dati
    public int getReturnValue() {return ReturnValue;}
    public DataInputStream getInn() {return inn;}
}
