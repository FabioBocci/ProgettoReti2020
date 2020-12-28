import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import Tools.*;
public class ClientMain extends RemoteObject implements NotifyEventInterface, ClientInterface {
    private static final long serialVersionUID = 1L;

    @Override
    public synchronized void notifyEventUser(String User, String Status) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public synchronized void notifyEventChat(String User, String msg) throws RemoteException {
        // TODO Auto-generated method stub

    }
    
}
