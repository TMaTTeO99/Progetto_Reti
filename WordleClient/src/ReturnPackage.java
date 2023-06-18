import java.io.DataInputStream;
import java.io.DataOutputStream;

//classe utilizzata per poter recuperare i valori di ritono che il server comunica al client, classe usata per poter far si
//che l eventuale doInBackground method possa inviare i vaolri coprretti all eventual emetodo done
public class ReturnPackage {

    private int ReturnValue;
    private DataInputStream inn;
    private DataOutputStream ou;

    public ReturnPackage(int RetVal, DataInputStream i, DataOutputStream o) {
        ReturnValue = RetVal;
        inn = i;
        ou = o;
    }
    public ReturnPackage(int RetVal, DataOutputStream o) {
        ReturnValue = RetVal;
        ou = o;
    }
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
    public DataOutputStream getOu() {return ou;}
}
