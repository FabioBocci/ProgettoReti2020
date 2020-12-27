import Exceptions.*;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.*;

public class ServerMain extends RemoteObject implements WorthServer, WorthServerRMI{

    private static final long serialVersionUID = 1L;
    Map<String, String> UserPass;
    Map<String , String>  UserState;
    List<Project> Progetti;


    public ServerMain()
    {
        //Da modificare con la lettura da un file
        UserPass=new HashMap<>();
        UserState = new HashMap<>();

        //Da modificare con lettura da file e cartella.
        Progetti = new ArrayList<>();
    }

    @Override
    public boolean Register(String Username, String Passw)throws RemoteException, IllegalArgumentException, NUserException {
        if(Username == Passw || Passw.length() < 6) throw new IllegalArgumentException();
        if(UserPass.get(Username) != null) throw new NUserException(Username);

        UserPass.put(Username, Passw);
        UserState.put(Username, "OFFLINE");
        return true;
    }


    @Override
    public void registerForCallBacks(NotifyEventInterface ClientInterface) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean Login(String Username, String Password) throws IllegalArgumentException, UserDontFoundException {
        // TODO Auto-generated method stub
        return false;
    }



    
}