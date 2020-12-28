package Tools;
import java.rmi.Remote;
import java.rmi.RemoteException;
public interface NotifyEventInterface extends Remote {

    //notifica per cambio di stato di un utente.
    public void notifyEventUser(String User,String Status)throws RemoteException;

    //TODO Chat ecc...
    //notifica per nuovi messaggi aggiunti
    public void notifyEventChat(String User,String msg) throws RemoteException;

}
