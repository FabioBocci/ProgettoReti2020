package Tools;
import java.rmi.Remote;
import java.rmi.RemoteException;
public interface NotifyEventInterface extends Remote {

    //notifica per cambio di stato di un utente.
    public void notifyEventUser(String User,String Status)throws RemoteException;


    //notifiche per aggiunta ad un nuovo progetto o la sua cancellazione
    public void notifyEventChat(String IP,int PORT,String PJT) throws RemoteException;
    public void notifyEventProjectCancel(String PJT) throws RemoteException;

}
