import Exceptions.*;
import Tools.*;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
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
    private static final int BUFFER_DIM = 4096;     //dimensione del Buffer per messaggi client-server
    private static final int porta = 1999;
    private static final String EXIT_CMD = "exit";
    private MulticastIpGenerator ip_gen;            //classe per generare IP di Multicast 

    List<Project> Progetti;                 //lista dei progetti
    ArrayList<User> UPlist;                 //arraylist degli utenti

    //private String ABS_PATH = "C:/Users/Fabio/Desktop/Progetto Reti Worth/Projects/";
    private String path = "./Projects/";

    public ServerMain() throws IOException {
        super();
        ip_gen= new MulticastIpGenerator(); 
        UPlist = new ArrayList<>();
        checkDBUsers();         //controllo se ci sono degli utenti salvati nel DB

        Progetti = new ArrayList<>();
        checkDBProject();       //controllo se ci sono dei progetti salvati nel DB
    }

    // Funzione che controlla e carica i Projetti gia presententi
    private void checkDBProject() throws IOException {
        File f = new File(path);
        if (!f.isDirectory())
            throw new IllegalArgumentException("Path not directory");
        File fs[] = f.listFiles();

        for (File file : fs) {
            if (file.isDirectory()) {

                Progetti.add(new Project(path, file.getName(),ip_gen.nextIP(),ip_gen.nextPORT()));
            }
        }
    }

    //Funzione che controlla e carica gli User-Password gia presenti nel DB
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
    //funzione che salva tutto il DB
    public synchronized void SaveAll() throws IOException {
        ObjectMapper om = new ObjectMapper();
        File users = new File(path + "User.json");
        om.writeValue(users, UPlist);

        for (Project p : Progetti) {
            p.SaveAll();
        }

        System.out.println("Tutto salvato correttamente");
    }

    //funzione chiamata attraverso l'interfaccia RMI per registrare un nuovo utente
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

    //funzione chiamata attraverso l'interfaccia RMI per registrare una interfaccia delle Callbacks del client
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

    //funzione chiamta attraverso l'intefaccia RMI per eliminare un interfaccia delle Callbacks del client
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

    //funzione utilizzata per effettuare il Login
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
    //funzione utilizzata per effetturare il logout
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
    //funzione utilizzata per creare un nuovo progetto
    //restituisce un errore se il progetto è gia presente o se l'utente non è presente nel DB
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
        Project prt = new Project(path, PJTname, ip_gen.nextIP(),ip_gen.nextPORT());

        usa.getNEI().notifyEventChat(prt.getIP(), prt.getPORT(), PJTname);
        prt.AddMember(User);
        Progetti.add(prt);
        
        return true;
    }
    //funzione utilizzata per terminare un progetto (se possibile)
    //restituisce un errore se utente non ha l'accesso a quel progetto, se il progetto non esiste, se il progetto ha ancora task non terminate
    @Override
    public synchronized boolean EndProject(String PJTname, String User) throws IllegalArgumentException, UserDontFoundException, ProjectDontFoundException,ProjectNotFinishableException {
        Project pkt=null;
        for (Project project : Progetti) {
            if(project.getName().equals(PJTname))pkt=project;
        }
        if(pkt==null)throw new ProjectDontFoundException(PJTname);
        if(!pkt.IsMember(User))throw new UserDontFoundException(User);
        if(!pkt.IsDone())throw new ProjectNotFinishableException("Progetto selezionato non terminato");
        for(String user : pkt.GetMember())
        {
            for(User us : UPlist)
            {
                try {
                        if(us.getUser().equals(user) && us.isOnline())
                            us.getNEI().notifyEventProjectCancel(PJTname);
                    } catch (RemoteException e) {

                        e.printStackTrace();
                    }
            }
        }
        pkt.delete();

        return true;
    }
    //funzione che restituisce una lista con il nome di tutti i progetti nel DB
    @Override
    public synchronized List<String> ListProject() {
        ArrayList<String> str = new ArrayList<String>();
        for (Project pj : Progetti) {
            str.add(pj.getName());
        }
        return str;
    }
    //funzione che permette di inserire un nuovo utente all'interno di un progetto
    //restituisce un errore se l'utente non è all'interno del DB o se il progetto non esiste
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

        try {
            if(usa.getNEI() != null)
                usa.getNEI().notifyEventChat(pjt.getIP(), pjt.getPORT(), PJTname);  //utente online
        } catch (RemoteException e) {

            e.printStackTrace();
        }
        pjt.AddMember(newUser);

        return true;
    }

    //restituisce una lista contente tutti gli utenti che partecipano al progetto
    //restituisce un errore se il progetto non esiste
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
    //restituisce una lista contente tutti i nomi delle card all'iinterno di un progetto
    //restituisce un errore se il progetto non esiste
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
    //restituisce una lista contenten tutte le info di una card
    //restituisce un errore se il progetto non esiste o se la card non è all'interno del progetto scelto
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
    //funzione che permettte di far cambiare stato alle card, se andato a buon fine manda un messaggio in chat dell'avvenuto spostamento
    //restituisce un errore se il progetto non esiste, se la card non fa parte del progetto o se il movimento è illegale
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

        //aggiungere alla chat un messaggio del server tipo->  SERVER: Task 'NameCard' Moved from 'Old' to 'New'
        try {
            MulticastSocket mui = new MulticastSocket(pjt.getPORT());
            InetAddress ia = InetAddress.getByName(pjt.getIP());
            mui.joinGroup(ia);
            byte[] buffer = ("SERVER: Task "+CARDname+" Moved from "+lstOLD+" to "+lstNEW).getBytes();
            DatagramPacket datagram = new DatagramPacket(buffer,buffer.length,ia,pjt.getPORT());
            mui.send(datagram);
            mui.leaveGroup(ia);
            mui.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
    //restituisce un lista contente tutta la storia di una card
    //restituisce un errore se il progetto non esiste o se la card non appartiene al progetto
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
    //funzione che aggiunge una nuova card al progetto con la relativa descizione, se andato a buon fine manda un chat un messaggio
    //restituisce un errore se il progetto non esiste o se la card è gia presente
    public synchronized boolean addCard(String PJTname, String CardName , String Desc)throws ProjectDontFoundException,IllegalArgumentException
    {
        Project pjt = null;
        for (Project pj : Progetti) {
            if (pj.getName().equals(PJTname)) {
                pjt = pj;
            }
        }
        if (pjt == null)
            throw new ProjectDontFoundException(PJTname);

        try {
            MulticastSocket mui = new MulticastSocket(pjt.getPORT());
            InetAddress ia = InetAddress.getByName(pjt.getIP());
            mui.joinGroup(ia);
            byte[] buffer = ("SERVER: Task "+CardName+" Aggiunta al progetto").getBytes();
            DatagramPacket datagram = new DatagramPacket(buffer,buffer.length,ia,pjt.getPORT());
            mui.send(datagram);
            mui.leaveGroup(ia);
            mui.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        pjt.AddCard(CardName, Desc);
        return true;
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
    //funzione chiamata dopo un login per aggiunge alla risposta le informaizioni su gli utenti ed i progetti
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
                result=result+"#"+project.getName()+"&&"+project.getIP()+"&&"+project.getPORT();
            }
        }
        return result;
    }
    //dopo aver letto un comando lo mando in esecuzione attraverso le funzioni sopra implementate
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
            case "addcard":
                try {
                    String desc = "DESC:";
                    for(int i = 3; i<stripped.length ; i++)
                        desc=desc+" "+stripped[i];
                    if(addCard(stripped[1],stripped[2],desc))
                        ris="CARD: "+stripped[2]+" creata, sotto progetto: "+stripped[1];
                    else
                        ris="Errore nella creazione";
                } catch (Exception e) {
                    ris=e.toString();
                }
                break;
            case "saveall":
                SaveAll();
                ris="tutto salvato! ";
                break;
            default:
                ris = "COMANDO NON RICONOSCIUTO";
        }
        //System.out.println("RIS="+ris);
        return ris;
    }

    public static void main(String[] args) throws IOException, AlreadyBoundException {
        ServerMain sm = new ServerMain();

        //Creo ed esporto il Server come RemoteObject
        WorthServerRMI stub = (WorthServerRMI) UnicastRemoteObject.exportObject(sm, 0);
        LocateRegistry.createRegistry(30000);
        Registry r = LocateRegistry.getRegistry(30000);

        r.bind("SERVER", stub);

        //Avvio del server TCP
        sm.start();


    }
    
}