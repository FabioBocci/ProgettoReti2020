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

    @Override
    public boolean Logout(String User) throws IllegalArgumentException, UserDontFoundException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean CreateProject(String PJTname, String User)
            throws IllegalArgumentException, UserDontFoundException, ProjectNameAlreadyUsed {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean EndProject(String PJTname, String User)
            throws IllegalArgumentException, UserDontFoundException, ProjectDontFoundException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> ListProject() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addMembers(String PJTname, String newUser)
            throws IllegalArgumentException, UserDontFoundException, ProjectDontFoundException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> ShowMembers(String PJTname) throws ProjectDontFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<List<Card>> ShowCards(String PJTname) throws ProjectDontFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Card ShowCard(String PJTname, String CARDname) throws ProjectDontFoundException, CardDontFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean MoveCard(String PJTname, String CARDname, String lstOLD, String lstNEW)
            throws ProjectDontFoundException, CardDontFoundException, IllegalMoveException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> GetCardHistory(String PJTname, String CARDname)
            throws ProjectDontFoundException, CardDontFoundException {
        // TODO Auto-generated method stub
        return null;
    }



    
}