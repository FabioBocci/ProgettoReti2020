import Exceptions.*;
import Tools.*;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServerMain extends RemoteObject implements WorthServer, WorthServerRMI {

    private static final long serialVersionUID = 1L;
    private static final int BUFFER_DIM = 4096;
    private static final int porta = 1999;
    private static final String EXIT_CMD = "exit";
    List<Project> Progetti;
    ArrayList<User> UPlist;

    //private String ABS_PATH = "C:/Users/Fabio/Desktop/Progetto Reti Worth/Projects/";
    private String path = "./Projects/";

    public ServerMain() throws IOException {
        super();
        UPlist = new ArrayList<>();
        checkDBUsers();

        Progetti = new ArrayList<>();
        checkDBProject();
    }

    // Funzione che controlla e carica i Projetti gia presententi
    private void checkDBProject() throws IOException {
        File f = new File(path);
        if (!f.isDirectory())
            throw new IllegalArgumentException("Path not directory");
        File fs[] = f.listFiles();

        for (File file : fs) {
            if (file.isDirectory()) {
                Progetti.add(new Project(path, file.getName()));
            }
        }
    }

    private void checkDBUsers() throws JsonParseException, JsonMappingException, IOException {
        File f = new File(path);
        if (!f.isDirectory())
            throw new IllegalArgumentException("Path not directory");
        File users = new File(path + "User.json");
        if (!users.exists())
            return;
        ObjectMapper om = new ObjectMapper();
        User[] UserList = {};
        UserList = om.readValue(users, User[].class);

        
        UPlist.addAll(Arrays.asList(UserList));
        
        //dopo aver letto gli utenti nel DB le metto tutti OFFLINE
        for (User user : UserList) {
            user.setOnline(false);
        }
    }

    public synchronized void SaveAll() throws IOException {
        ObjectMapper om = new ObjectMapper();
        File users = new File(path + "User.json");
        om.writeValue(users, UPlist);

        for (Project p : Progetti) {
            p.SaveAll();
        }

        System.out.println("Tutto salvato correttamente");
    }

    @Override
    public synchronized boolean Register(String Username, String Passw)
            throws RemoteException, IllegalArgumentException, NUserException {
        if (Username == Passw || Passw.length() < 6)
            throw new IllegalArgumentException();
        for (User user : UPlist) 
        {
            if(user.getUser().equals(Username)) throw new NUserException(Username);
        }
        
        NotifyEventInterface nei = null;
        for (User user : UPlist) {
            nei = user.getNEI();
            if(nei != null)
            {
                user.getNEI().notifyEventUser(Username, "OFFLINE");
            }
            nei=null;
        }
        UPlist.add(new User(Username, Passw));      //il costruttore di User lo inizializza gia a offline
        return true;
    }

    @Override
    public synchronized void registerForCallBacks(NotifyEventInterface ClientInterface,String User) throws RemoteException {
        User usa=null;
        for (User user : UPlist) {
            if(user.getUser().equals(User)) usa=user;
        }
        if(usa != null)
        {
            usa.setNEI(ClientInterface);
        }
    }

    @Override
    public synchronized void unregisterForCallback(NotifyEventInterface ClientInterface, String User) throws RemoteException {
        User usa=null;
        for (User user : UPlist) {
            if(user.getNEI() == ClientInterface) usa=user;
        }
        if(usa != null)
        {
            usa.setNEI(null);
        }
    }

    @Override
    public synchronized boolean Login(String Username, String Password)throws IllegalArgumentException, UserDontFoundException, RemoteException {
        User usa=null;
        for (User user : UPlist) {
            if(user.getUser().equals(Username)) usa=user;
        }
        if(usa == null)
        {
            throw new UserDontFoundException(Username);
        }
        if (Username == Password || Password.length() < 6)
            throw new IllegalArgumentException();

        if(usa.isOnline())return false;
        if(!usa.getPassword().equals(Password))return false;
        usa.setOnline(true);

        for (User user : UPlist) {
            if(usa != user && user.getNEI() != null)
                user.getNEI().notifyEventUser(Username, "ONLINE");
        }
        return true;
    }

    @Override
    public synchronized boolean Logout(String Username)throws IllegalArgumentException, UserDontFoundException, RemoteException {
        User usa=null;
        for (User user : UPlist) {
            if(user.getUser().equals(Username)) usa=user;
        }
        if(usa == null){throw new UserDontFoundException(Username);}
        if (!usa.isOnline()) throw new IllegalArgumentException("Utente selezionato gia OFFLINE");

        usa.setOnline(false);

        for (User user : UPlist) {
            if(usa != user && user.getNEI() != null)
                user.getNEI().notifyEventUser(Username, "OFFLINE");
        }
        return true;
    }

    @Override
    public synchronized boolean CreateProject(String PJTname, String User)throws IllegalArgumentException, UserDontFoundException, ProjectNameAlreadyUsed, IOException {
        User usa=null;
        for (User user : UPlist) {
            if(user.getUser().equals(User)) usa=user;
        }
        if(usa == null){throw new UserDontFoundException(User);}
        for (Project pj : Progetti) {
            if (pj.getName().equals(PJTname))
                throw new ProjectNameAlreadyUsed(PJTname);
        }
        Project prt = new Project(path, PJTname);
        prt.AddMember(User);
        Progetti.add(prt);
        
        return true;
    }

    @Override
    public synchronized boolean EndProject(String PJTname, String User) throws IllegalArgumentException, UserDontFoundException, ProjectDontFoundException {
        Project pkt=null;
        for (Project project : Progetti) {
            if(project.getName().equals(PJTname))pkt=project;
        }
        if(pkt==null)throw new ProjectDontFoundException(PJTname);
        if(!pkt.IsMember(User))throw new UserDontFoundException(User);
        pkt.delete();

        return true;
    }

    @Override
    public synchronized List<String> ListProject() {
        ArrayList<String> str = new ArrayList<String>();
        for (Project pj : Progetti) {
            str.add(pj.getName());
        }
        return str;
    }

    @Override
    public synchronized boolean addMembers(String PJTname, String newUser)throws IllegalArgumentException, UserDontFoundException, ProjectDontFoundException {
        User usa=null;
        for (User user : UPlist) {
            if(user.getUser().equals(newUser)) usa=user;
        }
        if(usa == null){throw new UserDontFoundException(newUser);}
        Project pjt = null;
        for (Project pj : Progetti) {
            if (pj.getName().equals(PJTname)) {
                pjt = pj;
            }
        }
        if (pjt == null)
            throw new ProjectDontFoundException(PJTname);

        pjt.AddMember(newUser);

        return true;
    }

    @Override
    public synchronized List<String> ShowMembers(String PJTname) throws ProjectDontFoundException {
        Project pjt = null;
        for (Project pj : Progetti) {
            if (pj.getName().equals(PJTname)) {
                pjt = pj;
            }
        }
        if (pjt == null)
            throw new ProjectDontFoundException(PJTname);

        return pjt.GetMember();
    }

    @Override
    public synchronized List<String> ShowCards(String PJTname) throws ProjectDontFoundException {
        Project pjt = null;
        for (Project pj : Progetti) {
            if (pj.getName().equals(PJTname)) {
                pjt = pj;
            }
        }
        if (pjt == null)
            throw new ProjectDontFoundException(PJTname);

        return pjt.GetCards();
    }

    @Override
    public synchronized List<String> ShowCard(String PJTname, String CARDname)
            throws ProjectDontFoundException, CardDontFoundException {
        Project pjt = null;
        for (Project pj : Progetti) {
            if (pj.getName().equals(PJTname)) {
                pjt = pj;
            }
        }
        if (pjt == null)
            throw new ProjectDontFoundException(PJTname);
        List<String> lst = pjt.GetCardInfo(CARDname);
        if (lst == null)
            throw new CardDontFoundException(CARDname);
        return lst;
    }

    @Override
    public synchronized boolean MoveCard(String PJTname, String CARDname, String lstOLD, String lstNEW)
            throws ProjectDontFoundException, CardDontFoundException, IllegalMoveException {
        Project pjt = null;
        for (Project pj : Progetti) {
            if (pj.getName().equals(PJTname)) {
                pjt = pj;
            }
        }
        if (pjt == null)
            throw new ProjectDontFoundException(PJTname);
        pjt.MoveCard(CARDname, lstOLD, lstNEW);
        return true;
    }

    @Override
    public synchronized List<String> GetCardHistory(String PJTname, String CARDname)
            throws ProjectDontFoundException, CardDontFoundException {
        Project pjt = null;
        for (Project pj : Progetti) {
            if (pj.getName().equals(PJTname)) {
                pjt = pj;
            }
        }
        if (pjt == null)
            throw new ProjectDontFoundException(PJTname);
        List<String> lst = pjt.getCardHistory(CARDname);
        if (lst == null)
            throw new CardDontFoundException(CARDname);
        return lst;
    }

    public void start() {
        String Ris = "";
        String Command = "";

        // Creo il server e faccio il Bind sulla port 1999
        try (ServerSocketChannel server = ServerSocketChannel.open();) {
            server.socket().bind(new InetSocketAddress(porta));
            server.configureBlocking(false);


            // creo il selector e lo collego al server
            Selector selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
            
            System.out.println("Server online");

            while (true) {
                if (selector.select() == 0)
                    continue;
    
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
    
                    try {
                        
                        if (key.isAcceptable()) {
                            ServerSocketChannel ss = (ServerSocketChannel) key.channel();
                            ByteBuffer atch = ByteBuffer.allocate(BUFFER_DIM);
                            SocketChannel client = ss.accept();
                            System.out.println("Connessione Accettata da :" + client);
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ,atch);
                        }
                        if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            ByteBuffer read = (ByteBuffer) key.attachment();
                            client.read(read);

                            Command = new String(read.array()).trim();
                            read.clear();

                            System.out.println("Letto da: " + client + " | Comando " + Command);

                            try {
                                Ris = execute(Command); // Eseguo il comando ricevuto dal Client e preparo la risposta
                            } catch (Exception e) {
                                Ris = "ERRORE " + e.toString();
                            }
                            Command="";

                            client.register(selector, SelectionKey.OP_WRITE,Ris);
                        }
                        if (key.isWritable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            String risposta =(String) key.attachment();
                            ByteBuffer out = ByteBuffer.wrap(risposta.getBytes());
                            
                            client.write(out);
                            out.clear();

                            client.register(selector, SelectionKey.OP_READ,ByteBuffer.allocate(BUFFER_DIM));
                        }
                    } catch (IOException e) {
                        key.cancel();
                        try{key.channel().close();}
                        catch(IOException ign){}
                    }
                    //check exit command
                    if (Ris.equals(EXIT_CMD))
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
 
    }
    private String loginADD(String LogUser)
    {
        String result="";


        //creo la lista di utenti con stato
        for (User user : UPlist) {
            if(user.isOnline())
            result=result+user.getUser()+"-ONLINE ";
            else
            result=result+user.getUser()+"-OFFLINE ";
        }


        for (Project project : Progetti) {
            if(project.GetMember().contains(LogUser))
            {
                result=result+"#"+project.getName()+"&&"+"FAKEIP";
            }
        }
        return result;
    }

    private String execute(String cmd)
            throws IllegalArgumentException, UserDontFoundException, ProjectNameAlreadyUsed, IOException {
        String[] stripped = cmd.split(" ");
        stripped[0] = stripped[0].toLowerCase();
        String ris = "";

        switch (stripped[0]) {
            case "login":
                if (Login(stripped[1], stripped[2]))
                    ris = "Login effettuato correttamente. " + stripped[1]+"#"+loginADD(stripped[1]);
                else
                    ris = "Login Errato.";
                break;
            case "logout":
                if (Logout(stripped[1]))
                    ris = "Logout effettuato correttamente. " + stripped[1];
                else
                    ris = "Logout Errato. ";
                break;
            case "createproject":
                if (CreateProject(stripped[1], stripped[2]))
                    ris = "Progetto creato. " + stripped[1];
                else
                    ris = "...";
                break;
            case "endproject":
                if (EndProject(stripped[1], stripped[2]))
                    ris = "Progetto terminato. " + stripped[1];
                else
                    ris = "---";
                break;
            case "listproject":
                ris = ListProject().toString();
                break;
            case "addmember":
                if (addMembers(stripped[1], stripped[2]))
                    ris = "User " + stripped[2] + " Aggiunto su " + stripped[1];
                else
                    ris = "---";
                break;
            case "showmember":
                ris = ShowMembers(stripped[1]).toString();
                break;
            case "showcards":
                ris = ShowCards(stripped[1]).toString();
                break;
            case "showcard":
                ris = ShowCard(stripped[1], stripped[2]).toString();
                break;
            case "getcardhistory":
                ris = GetCardHistory(stripped[1], stripped[2]).toString();
                break;
            case "movecard":
                if (MoveCard(stripped[1], stripped[2], stripped[3], stripped[4]))
                    ris = "Movimento effettuato " + stripped[2] + " da " + stripped[3] + " a " + stripped[4];
                else
                    ris = "---";
                break;
            default:
                ris = "COMANDO NON RICONOSCIUTO";
        }
        //System.out.println("RIS="+ris);
        return ris;
    }

    public static void main(String[] args) throws IOException, AlreadyBoundException {
        ServerMain sm = new ServerMain();
       /*  
        sm.Register("Fabio", "01234567");
        sm.Register("Mario", "01234567");
        
        String str_string = sm.execute("listproject");
        str_string = str_string.replace("[", "");
        str_string = str_string.replace(" ", "");
        str_string = str_string.replace("]", "");
        List<String> myList = new ArrayList<String>(Arrays.asList(str_string.split(",")));
        System.out.println(str_string);
        System.out.println(myList);
        //sm.SaveAll(); */

        WorthServerRMI stub = (WorthServerRMI) UnicastRemoteObject.exportObject(sm, 0);
        LocateRegistry.createRegistry(30000);
        Registry r = LocateRegistry.getRegistry(30000);

        r.bind("SERVER", stub);

        /* Scanner sc = new Scanner(System.in);
        while(true)
        {
            String cmd = sc.nextLine();
            String res = sm.execute(cmd);

            System.out.println("RES= "+res);

        } */
        sm.start();


    }
    
}