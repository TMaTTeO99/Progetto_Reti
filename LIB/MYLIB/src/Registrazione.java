import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface Registrazione extends Remote {

    public int registra(byte [] username, byte [] passwd, UUID ID) throws RemoteException;
    public void RegistryForCallBack(String username, NotificaClient stub, UUID ID) throws RemoteException;
    public void UnRegistryForCallBack(String username, NotificaClient stub, UUID ID)throws RemoteException;
}
