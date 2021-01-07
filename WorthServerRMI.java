import java.rmi.*;
import Exceptions.*;
import Tools.*;
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

    //Funzione per la registrazione delle NEI degli Utenti
    public void registerForCallBacks(NotifyEventInterface ClientInterface,String User) throws RemoteException;

    //Funzione per l'eliminazione delle NEI degli Utenti
    public void unregisterForCallback(NotifyEventInterface ClientInterface,String User) throws RemoteException;
}
