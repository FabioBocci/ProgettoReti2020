package Tools;
import java.rmi.Remote;
import java.rmi.RemoteException;
public interface NotifyEventInterface extends Remote {

    //notifica per cambio di stato di un utente.
    public void notifyEventUser(String User,String Status)throws RemoteException;


    //Notifica per l'aggiunta di un nuovo progetto, con aggiunta di IP e PORT per la chat del relativo progetto
    public void notifyEventChat(String IP,int PORT,String PJT) throws RemoteException;

    //Notifica per la rimozione di un progetto, (quando un progetto termina il client si deve disconnettere dalla chat)
    public void notifyEventProjectCancel(String PJT) throws RemoteException;

}
