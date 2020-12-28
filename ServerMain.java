import Exceptions.*;
import Tools.*;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServerMain extends RemoteObject implements WorthServer, WorthServerRMI {

    private static final long serialVersionUID = 1L;
    Map<String, String> UserState;
    List<Project> Progetti;
    UsersPass[] UsersPassList = {};

    private String ABS_PATH = "C:/Users/Fabio/Desktop/Progetto Reti Worth/Projects/";
    private String path = "/Projects/";
    private List<NotifyEventInterface> clients;

    public ServerMain() throws IOException {
        super();

        UserState = new HashMap<>();
        checkDBUsers();

        if(UsersPassList.length!= 0 )
        {
            for(UsersPass up : UsersPassList)
            {
                UserState.put(up.getUser(), "OFFLINE");
            }
        }
        Progetti = new ArrayList<>();
        checkDBProject();
    }

    // Funzione che controlla e carica i Projetti gia presententi
    private void checkDBProject() throws IOException {
        File f = new File(ABS_PATH);
        if (!f.isDirectory())
            throw new IllegalArgumentException("Path not directory");
        File fs[] = f.listFiles();

        for (File file : fs) {
            if (file.isDirectory()) {
                Progetti.add(new Project(ABS_PATH, file.getName()));
            }
        }
    }

    private void checkDBUsers() throws JsonParseException, JsonMappingException, IOException
    {
        File f = new File(ABS_PATH);
        if(!f.isDirectory()) throw new IllegalArgumentException("Path not directory");
        File users = new File(path+"UsersPass.json");
        if(!users.exists()) return;
        ObjectMapper om = new ObjectMapper();
        UsersPassList= om.readValue(users, UsersPass[].class);
    }

    public void SaveAll() throws IOException
    {
        ObjectMapper om = new ObjectMapper();
        File users = new File(ABS_PATH+"UsersPass.json");
        om.writeValue(users, UsersPassList);

        for(Project p : Progetti)
        {
            p.SaveAll();
        }

        System.out.println("Tutto salvato correttamente");
    }

    @Override
    public boolean Register(String Username, String Passw)throws RemoteException, IllegalArgumentException, NUserException {
        if(Username == Passw || Passw.length() < 6) throw new IllegalArgumentException();
        if(UserState.get(Username) != null) throw new NUserException(Username);

        UsersPass[] tmp = new UsersPass[UsersPassList.length+1];
        int i=0;
        for(UsersPass up : UsersPassList)
        {
            tmp[i]=up;
            i++;
        }
        tmp[i]= new UsersPass(Username, Passw);

        UserState.put(Username, "OFFLINE");
        return true;
    }


    @Override
    public void registerForCallBacks(NotifyEventInterface ClientInterface) throws RemoteException {
         if (!clients.contains(ClientInterface)) {
            clients.add(ClientInterface);
            System.out.println("New client registered.");
        }

    }

    @Override
    public void unregisterForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
        if (clients.remove(ClientInterface)) {
            System.out.println("Client unregistered");
        } else {
            System.out.println("Unable to unregister client.");
        }
    }

    @Override
    public boolean Login(String Username, String Password) throws IllegalArgumentException, UserDontFoundException, RemoteException {
        if(!UserState.keySet().contains(Username))throw new UserDontFoundException(Username);
        if(Username == Password || Password.length() < 6) throw new IllegalArgumentException();

        boolean found = false;
        int i=0;
        while(!found && i<UsersPassList.length)
        {
            if(UsersPassList[i].getUser().equals(Username) && UsersPassList[i].getPassword().equals(Password))
            {
                found=true;
            }
        }
        if(!found)
            return false;
        
        for(NotifyEventInterface nei : clients)
        {
            nei.notifyEventUser(Username, "ONLINE");
        }
        return true;
    }

    @Override
    public boolean Logout(String Username) throws IllegalArgumentException, UserDontFoundException, RemoteException {
        if(!UserState.keySet().contains(Username))throw new UserDontFoundException(Username);
        if(UserState.get(Username).equals("OFFLINE"))throw new IllegalArgumentException("Utente selezionato gia OFFLINE");

        UserState.put(Username, "OFFLINE");
        
        for(NotifyEventInterface nei : clients)
        {
            nei.notifyEventUser(Username, "OFFLINE");
        }
        return true;
    }

    @Override
    public boolean CreateProject(String PJTname, String User)throws IllegalArgumentException, UserDontFoundException, ProjectNameAlreadyUsed,IOException {
        if(!UserState.keySet().contains(User))throw new UserDontFoundException(User);
        for (Project pj : Progetti) {
            if(pj.getName().equals(PJTname))throw new ProjectNameAlreadyUsed(PJTname);
        }
        Project prt = new Project(ABS_PATH, PJTname);
        Progetti.add(prt);

        return true;
    }

    @Override
    public boolean EndProject(String PJTname, String User) throws IllegalArgumentException, UserDontFoundException, ProjectDontFoundException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> ListProject() {
        ArrayList<String> str= new ArrayList<String>();
        for (Project pj : Progetti) {
            str.add(pj.getName());
        }
        return str;
    }

    @Override
    public boolean addMembers(String PJTname, String newUser)throws IllegalArgumentException, UserDontFoundException, ProjectDontFoundException {
        if(!UserState.keySet().contains(newUser))throw new UserDontFoundException(newUser);
        Project pjt=null;
        for (Project pj : Progetti) {
            if(pj.getName().equals(PJTname))
            {
                pjt=pj;
            }
        }
        if(pjt==null)throw new ProjectDontFoundException(PJTname);

        pjt.AddMember(newUser);

        return true;
    }

    @Override
    public List<String> ShowMembers(String PJTname) throws ProjectDontFoundException 
    {
        Project pjt=null;
        for (Project pj : Progetti) {
            if(pj.getName().equals(PJTname))
            {
                pjt=pj;
            }
        }
        if(pjt==null)throw new ProjectDontFoundException(PJTname);

        return pjt.GetMember();
    }

    @Override
    public List<String> ShowCards(String PJTname) throws ProjectDontFoundException {
        Project pjt=null;
        for (Project pj : Progetti) {
            if(pj.getName().equals(PJTname))
            {
                pjt=pj;
            }
        }
        if(pjt==null)throw new ProjectDontFoundException(PJTname);

        return pjt.GetCards();
    }

    @Override
    public List<String> ShowCard(String PJTname, String CARDname) throws ProjectDontFoundException, CardDontFoundException {
        Project pjt=null;
        for (Project pj : Progetti) {
            if(pj.getName().equals(PJTname))
            {
                pjt=pj;
            }
        }
        if(pjt==null)throw new ProjectDontFoundException(PJTname);
        List<String> lst =pjt.GetCardInfo(CARDname);
        if(lst==null)throw new CardDontFoundException(CARDname);
        return lst;
    }

    @Override
    public boolean MoveCard(String PJTname, String CARDname, String lstOLD, String lstNEW) throws ProjectDontFoundException, CardDontFoundException, IllegalMoveException {
        Project pjt=null;
        for (Project pj : Progetti) {
            if(pj.getName().equals(PJTname))
            {
                pjt=pj;
            }
        }
        if(pjt==null)throw new ProjectDontFoundException(PJTname);
        pjt.MoveCard(CARDname, lstOLD, lstNEW);
        return true;
    }

    @Override
    public List<String> GetCardHistory(String PJTname, String CARDname)throws ProjectDontFoundException, CardDontFoundException {
        Project pjt=null;
        for (Project pj : Progetti) {
            if(pj.getName().equals(PJTname))
            {
                pjt=pj;
            }
        }
        if(pjt==null)throw new ProjectDontFoundException(PJTname);
        List<String> lst =pjt.getCardHistory(CARDname);
        if(lst==null)throw new CardDontFoundException(CARDname);
        return lst;
    }


    public static void main(String[] args) throws IOException {
        ServerMain sm = new ServerMain();
        
        sm.Register("Fabio", "01234567");
        sm.SaveAll();
        System.out.println(sm.ListProject());
        //System.out.println(sm.UsersPassList[0]);
        //System.out.println(sm.UsersPassList[0].getUser()+" | "+sm.UsersPassList[0].getPassword());
    }
    
}