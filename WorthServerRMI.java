import java.rmi.*;
import Exceptions.*;
public interface WorthServerRMI extends Remote {
    /**
     * @param Username != Passw AND non presente nel programma
     * @param Passw != Username AND Passw.leght() > 5
     * @effect Aggiunge un nuovo User alla lista degli utenti 
     * @return true se è andato a buon fine
     * @throws RemoteException
     * @throws IllegalArgumentException if Username == Passw || Passw.leght() < 6
     * @throws NUserException  if UserList.contain(Username) == True
     */
    public boolean Register(String Username, String Passw)throws RemoteException,IllegalArgumentException,NUserException;


    public void registerForCallBacks(NotifyEventInterface ClientInterface) throws RemoteException;
    public void unregisterForCallback(NotifyEventInterface ClientInterface) throws RemoteException;
}
