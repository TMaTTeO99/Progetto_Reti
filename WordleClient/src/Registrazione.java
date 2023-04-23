import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Registrazione extends Remote {

    public int registra(String username, String passwd, NotificaClient stub) throws RemoteException;

}
