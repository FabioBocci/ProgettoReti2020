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
    private static final int DIM_BUFFER = 4096;
    private static final int porta = 1999;
    private static final String EXIT_CMD = "exit";
    Map<String, String> UserState;
    List<Project> Progetti;
    ArrayList<UsersPass> UPlist;

    private String ABS_PATH = "C:/Users/Fabio/Desktop/Progetto Reti Worth/Projects/";
    private String path = "/Projects/";
    private List<NotifyEventInterface> clients;

    public ServerMain() throws IOException {
        super();
        UPlist = new ArrayList<>();
        UserState = new HashMap<>();
        checkDBUsers();

        if (UPlist.size() != 0) {
            for (UsersPass up : UPlist) {
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

    private void checkDBUsers() throws JsonParseException, JsonMappingException, IOException {
        File f = new File(ABS_PATH);
        if (!f.isDirectory())
            throw new IllegalArgumentException("Path not directory");
        File users = new File(path + "UsersPass.json");
        if (!users.exists())
            return;
        ObjectMapper om = new ObjectMapper();
        UsersPass[] UsersPassList = {};
        UsersPassList = om.readValue(users, UsersPass[].class);
        UPlist.addAll(Arrays.asList(UsersPassList));
    }

    public synchronized void SaveAll() throws IOException {
        ObjectMapper om = new ObjectMapper();
        File users = new File(ABS_PATH + "UsersPass.json");
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
        if (UserState.get(Username) != null)
            throw new NUserException(Username);

        UPlist.add(new UsersPass(Username, Passw));

        UserState.put(Username, "OFFLINE");
        return true;
    }

    @Override
    public synchronized void registerForCallBacks(NotifyEventInterface ClientInterface) throws RemoteException {
        if (!clients.contains(ClientInterface)) {
            clients.add(ClientInterface);
            System.out.println("New client registered.");
        }

    }

    @Override
    public synchronized void unregisterForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
        if (clients.remove(ClientInterface)) {
            System.out.println("Client unregistered");
        } else {
            System.out.println("Unable to unregister client.");
        }
    }

    @Override
    public synchronized boolean Login(String Username, String Password)
            throws IllegalArgumentException, UserDontFoundException, RemoteException {
        if (!UserState.keySet().contains(Username))
            throw new UserDontFoundException(Username);
        if (Username == Password || Password.length() < 6)
            throw new IllegalArgumentException();

        boolean found = false;
        int i = 0;
        while (!found && i < UPlist.size()) {
            if (UPlist.get(i).getUser().equals(Username) && UPlist.get(i).getPassword().equals(Password)) {
                found = true;
            }
        }
        if (!found)
            return false;

        for (NotifyEventInterface nei : clients) {
            nei.notifyEventUser(Username, "ONLINE");
        }
        return true;
    }

    @Override
    public synchronized boolean Logout(String Username)
            throws IllegalArgumentException, UserDontFoundException, RemoteException {
        if (!UserState.keySet().contains(Username))
            throw new UserDontFoundException(Username);
        if (UserState.get(Username).equals("OFFLINE"))
            throw new IllegalArgumentException("Utente selezionato gia OFFLINE");

        UserState.put(Username, "OFFLINE");

        for (NotifyEventInterface nei : clients) {
            nei.notifyEventUser(Username, "OFFLINE");
        }
        return true;
    }

    @Override
    public synchronized boolean CreateProject(String PJTname, String User)
            throws IllegalArgumentException, UserDontFoundException, ProjectNameAlreadyUsed, IOException {
        if (!UserState.keySet().contains(User))
            throw new UserDontFoundException(User);
        for (Project pj : Progetti) {
            if (pj.getName().equals(PJTname))
                throw new ProjectNameAlreadyUsed(PJTname);
        }
        Project prt = new Project(ABS_PATH, PJTname);
        Progetti.add(prt);

        return true;
    }

    @Override
    public synchronized boolean EndProject(String PJTname, String User)
            throws IllegalArgumentException, UserDontFoundException, ProjectDontFoundException {
        // TODO Auto-generated method stub
        return false;
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
    public synchronized boolean addMembers(String PJTname, String newUser)
            throws IllegalArgumentException, UserDontFoundException, ProjectDontFoundException {
        if (!UserState.keySet().contains(newUser))
            throw new UserDontFoundException(newUser);
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

    public void start() throws IOException {
        String Ris = "";
        String Command = "";
        ByteBuffer input;
        ByteBuffer out;

        // Creo il server e faccio il Bind sulla port 1999
        ServerSocketChannel server = ServerSocketChannel.open();
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

                if (key.isAcceptable()) {
                    ServerSocketChannel ss = (ServerSocketChannel) key.channel();
                    SocketChannel client = ss.accept();
                    System.out.println("Connessione Accettata da :" + client);
                    client.configureBlocking(false);
                    SelectionKey key2 = client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    out = ByteBuffer.allocate(DIM_BUFFER);
                    out.clear();
                    key2.attach(out);
                }
                if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    input = (ByteBuffer) key.attachment();

                    if (input.hasRemaining()) {
                        input.compact();
                        input.mark();
                        client.read(input);
                        input.reset();
                    } else {
                        input.clear();
                        client.read(input);
                    }
                    Command = new String(input.array()).trim();
                    System.out.println("Letto da: " + client + " | Comdando " + Command);
                    try {
                        Ris = execute(Command); // Eseguo il comando ricevuto dal Client e preparo la risposta
                    } catch (Exception e) {
                        Ris = "ERRORE " + e.toString();
                    }
                }
                if (key.isWritable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    out = ByteBuffer.wrap(Ris.getBytes());
                    out.flip();
                    client.write(out);
                    System.out.println("Mando a: " + client + " | Risposta " + Ris);
                }
                key.cancel();
                if (Ris.equals(EXIT_CMD))
                    break;
            }
        }
    }

    private String execute(String cmd)
            throws IllegalArgumentException, UserDontFoundException, ProjectNameAlreadyUsed, IOException {
        String[] stripped = cmd.split(" ");
        stripped[0] = stripped[0].toLowerCase();
        String ris = "";

        switch (stripped[0]) {
            case "login":
                if (Login(stripped[1], stripped[2]))
                    ris = "Login effettuato correttamente. " + stripped[1];
                else
                    ris = "Login Errato";
                break;
            case "logout":
                if (Logout(stripped[1]))
                    ris = "Logout effettuato correttamente. " + stripped[1];
                else
                    ris = "Logout Errato";
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


        sm.start();


    }
    
}