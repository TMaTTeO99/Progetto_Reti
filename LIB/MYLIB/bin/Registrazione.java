import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Registrazione extends Remote {

    public int registra(byte [] username, byte [] passwd, int ID) throws RemoteException;
    public void RegisryForCallBack(String username, NotificaClient stub) throws RemoteException;
    public void UnRegisryForCallBack(String username, NotificaClient stub)throws RemoteException;
}
