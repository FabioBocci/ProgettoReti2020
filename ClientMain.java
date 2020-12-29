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
    public void notifyEventChat(String IP, int PORT) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void notifyEventProjectCancel(String address, int port) throws RemoteException {
        // TODO Auto-generated method stub

    }
    
}
