import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import Exceptions.NUserException;
import Tools.*;

public class ClientMain extends RemoteObject implements NotifyEventInterface{

    // private static final String EXIT_CMD = "exit";
    private static final long serialVersionUID = 1L;
    private static final int BUFFER_DIM = 4096;

    private String IP = "127.0.0.1";
    private int PORT = 1999;

    private boolean EXIT = false;

    private Map<String, String> UserState; // Contiene le informazioni Username-State

    //private Map<String, MessagingQueue> PJ_ChatQ; 
    private Map<String, ChatThread> PJ_Chat;    //contiene il nome del progetto come chiave e il thead della chat come valore

    private WorthServerRMI serverRMI; // Interfaccia remota del Server
    private NotifyEventInterface nei; // Interfaccia forCallBacks

    private Scanner scan; // Scanner per leggere da tastiera

    private String UserName; // se un utente è Loggato allora contiene il nomeUtente
    private Boolean LOGGED; // True se un utente è loggato False altrimenti

    private Object lock_project;    //object utilizzato per lock sui progetti
    private Object lock_user;       //object utilizzato per lock su gli utenti

    public ClientMain(String ip, int Port) throws RemoteException {
        super();
        this.IP = ip;                       //ip del server
        this.PORT = Port;                   //porta in cui andarsi a connettere al server
        this.scan = new Scanner(System.in); //scanner per leggere i messaggi da tastiera
        this.LOGGED = false;                //boolean per controllare che siamo loggati o no
        this.nei = (NotifyEventInterface) UnicastRemoteObject.exportObject(this, 0);    //creo l'interfaccia per le callbacks da passare al server
        
        this.UserState = new HashMap<>();   //map per User-State (online/offline)
        this.PJ_Chat = new HashMap<>();     //map per ProjectName-ChatThread
        //this.PJ_ChatQ = new HashMap<>();

        lock_project = new Object();        //object per lock sui progetti
        lock_user = new Object();           //object per lock sugli utenti 
    }

    //funzione chiamata dal server quando un utente cambia stato (online-offline)
    @Override
    public void notifyEventUser(String User, String Status) throws RemoteException {
        synchronized(lock_user)
        {
            UserState.put(User, Status);
        }
    }

    //funzione chiamata dal server quando si viene aggiunti ad un nuovo progetto
    @Override
    public void notifyEventChat(String IP, int PORT, String PJT) throws RemoteException {
        synchronized(lock_project)
        {
            try {
                MulticastSocket mus = new MulticastSocket(PORT);
                InetAddress group = InetAddress.getByName(IP);
                ChatThread ct = new ChatThread(mus, group, PORT);
                PJ_Chat.put(PJT, ct);
                ct.start();
    
                System.out.println("AGGIUNTO ALLA CHAT DEL PROGETTO: "+PJT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        

    }

    //funzione chiamata dal server quando un progetto viene terminato
    @Override
    public void notifyEventProjectCancel(String pjt) throws RemoteException {
        synchronized(lock_project)
        {
            PJ_Chat.get(pjt).interrupt();
            PJ_Chat.remove(pjt);
            System.out.println("PROGETTO TERMINATO: "+pjt);
        }

    }

    private String getNewCommand() {
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Inserisci un nuovo comando:     type help for HELP ");
        return scan.nextLine();
    }
    //funzione che serve per gestire i comandi ed inviarli al server dopo aver fatto i controlli base
    public void gestCommand(String Command, SocketChannel client) throws IOException {
        String[] stripped_comm = Command.split(" ");
        switch (stripped_comm[0].toLowerCase()) {
            case "register":
                if(!LOGGED)
                    register(stripped_comm[1], stripped_comm[2]);
                else
                    System.out.println("Puoi invocare questo comando solo da Sloggato");
                break;
            case "login":
                if (!LOGGED)
                    login_Command(Command, client);
                else
                    System.out.println("User Already Logged:" + UserName);
                break;
            case "logout":
                if (LOGGED)
                    if(CheckName(stripped_comm[1]))
                        logout_Command(Command, client);
                    else
                        System.out.println("ERRORE: Nome non riconosciuto");
                else
                    System.out.println("User NOT Logged");
                break;
            case "createproject":
                if (LOGGED)
                    generic_Command(Command+" "+UserName, client);
                else
                    System.out.println("User NOT Logged");
                break;
            case "endproject":
                if (LOGGED)
                    if(CheckPJT(stripped_comm[1]))
                        generic_Command(Command+" "+UserName, client);
                    else
                        System.out.println("ERRRO: controlla di avere l'accesso e di aver inserito i dati giusti");
                else
                    System.out.println("User NOT Logged");
                break;
            case "listproject":
                if (LOGGED)
                    generic_Command(Command, client);
                else
                    System.out.println("User NOT Logged");
                break;
            case "addmember":
                if (LOGGED)
                    if(CheckPJT(stripped_comm[1]) && CheckUser(stripped_comm[2]))
                        generic_Command(Command, client);
                    else
                        System.out.print("ERRORE: controlla di aver l'accesso e di aver inserito i dati giusti");
                else
                    System.out.println("User NOT Logged");
                break;
            case "showmember":
                if (LOGGED)
                    if(CheckPJT(stripped_comm[1]))
                        generic_Command(Command, client);
                    else
                        System.out.println("ERRORE: controlla di aver l'accesso e di aver inserito i dati giusti");
                else
                    System.out.println("User NOT Logged");
                break;
            case "showcards":
                if (LOGGED)
                    if(CheckPJT(stripped_comm[1]))
                        generic_Command(Command, client);
                    else
                        System.out.println("ERRORE: controlla di aver l'accesso e di aver inserito i dati giusti");
                else
                    System.out.println("User NOT Logged");
                break;
            case "showcard":
                if (LOGGED)
                    if(CheckPJT(stripped_comm[1]))
                        generic_Command(Command, client);
                    else
                        System.out.println("ERRORE: controlla di aver l'accesso e di aver inserito i dati giusti");
                else
                    System.out.println("User NOT Logged");
                break;
            case "getcardhistory":
                if (LOGGED)
                    if(CheckPJT(stripped_comm[1]))
                        generic_Command(Command, client);
                    else
                        System.out.println("ERRORE: controlla di aver l'accesso e di aver inserito i dati giusti");
                else
                    System.out.println("User NOT Logged");
                break;
            case "movecard":
                if (LOGGED)
                    if(CheckPJT(stripped_comm[1]))
                        generic_Command(Command, client);
                    else
                        System.out.println("ERRORE: controlla di aver l'accesso e di aver inserito i dati giusti");

                else
                    System.out.println("User NOT Logged");
                break;
            case "addcard":
                if(LOGGED)
                    if(CheckPJT(stripped_comm[1]))
                        generic_Command(Command, client);
                    else
                        System.out.println("ERRORE: controlla di aver l'accesso e di aver inserito i dati giusti");
                else
                    System.out.println("User NOT Logged");
                break;
            case "exit":
                EXIT = true;
                break;
            case "help":
                help();
                break;
            case "readchat":
                if (LOGGED)
                    if(CheckPJT(stripped_comm[1]))
                        readMSG(stripped_comm[1]);
                    else
                        System.out.println("ERRORE: controlla di aver l'accesso e di aver inserito i dati giusti");
                else
                    System.out.println("User NOT Logged");
                break;
            case "sendmsg":
                if (LOGGED)
                    if(CheckPJT(stripped_comm[1]))
                        {
                            String msgN=UserName+":";
                            for(int i = 2; i<stripped_comm.length; i++)
                            {
                                msgN=msgN+" "+stripped_comm[i];
                            }
                            sendMSG(stripped_comm[1], msgN);
                        }
                    else
                        System.out.println("ERRORE: controlla di aver l'accesso e di aver inserito i dati giusti");
                else
                    System.out.println("User NOT Logged");
                break;
            case "listuser":
                if (LOGGED)
                    System.out.print(UserState.keySet());
                else
                    System.out.println("User NOT Logged");
                break;
            case "listuseronline":
            if(LOGGED)
            {
                synchronized(lock_user)
                {
                    for (String use : UserState.keySet()) {
                        if (UserState.get(use).contains("ONLINE"))
                            System.out.print(use + " ");
                    }
                    System.out.println("");
                }
            }
            else
                System.out.println("User not logged");
            break;
            case "saveall":
                generic_Command(Command, client);
                break;
            default:
                System.out.println("Comando non riconosciuto");
        }
    }
    //Funzione utilizata per mandare un messaggio in una chat
    private void sendMSG(String pjt, String msg) {
        try {
            this.PJ_Chat.get(pjt).sendMsg(msg);
            System.out.println("Messaggio inviato con successo");
        } catch (NullPointerException | IOException e) 
        {
            e.printStackTrace();
        }
    }
    //funzione utilizzata per leggere i messaggi da una chat
    private void readMSG(String pjt)
    {
        List<String> str = this.PJ_Chat.get(pjt).readMsg();
        System.out.println("------------------");
        for (String string : str) {
            System.out.println(string);
        }
        System.out.println("------------------");
    }
    private void register(String user, String password) {
        try {
            if(serverRMI.Register(user, password))
                System.out.println("Utente registrato");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NUserException e) {
            e.printStackTrace();
        }
    }

    private void help() {
        System.out.println("----------------------------WELCOME TO HELP DESK------------------------");
        System.out.println("Ecco una lista dei comandi che puoi usare:                              ");
        System.out.println("Register 'UserName' 'Password'                                      (**)");          //DONE
        System.out.println("Login 'Username' 'Password'                                         (**)");          //DONE
        System.out.println("Logout 'Username'                                                       ");          //DONE
        System.out.println("listUser                                                                ");          //DONE
        System.out.println("listUserOnline                                                          ");          //DONE
        System.out.println("CreateProject  'ProjectName'                                            ");          //DONE
        System.out.println("listProject                                                             ");          //DONE 
        System.out.println("addMember 'ProjectName' 'NewUserName'                                (*)");          //DONE
        System.out.println("ShowMember 'ProjectName'                                             (*)");          //DONE
        System.out.println("ShowCards 'ProjectName'                                              (*)");          //DONE
        System.out.println("ShowCard 'ProjectName' 'CardName'                                    (*)");          //DONE
        System.out.println("AddCard 'ProjectName' 'CardName' 'DESC'                              (*)");          //DONE
        System.out.println("MoveCard 'ProjectName' 'CardName' 'OldState' 'NewState'                 ");          //DONE
        System.out.println("GetCardHistory 'ProjectName' 'CardName'                              (*)");          //DONE
        System.out.println("ReadChat 'ProjectName'                                               (*)");          //DONE
        System.out.println("SendMSG 'ProjectName' 'MSG'                                          (*)");          //DONE
        System.out.println("EndProject 'ProjectName'                                             (*)");          //DONE
        System.out.println("Exit                                                                    ");          //DONE
        System.out.println("Help                                                                    ");          //DONE
        System.out.println("-------------------------------------NB:--------------------------------");
        System.out.println(" (**) Utente non deve essere Loggato ");
        System.out.println(" (*)  Utente deve appartenere ai membri di quel progetto ");
        System.out.println(" per tutti i comandi (ad eccesioni di quelli con (**)) devi essere loggato per utilizzarli ");
        //System.out.println("");
        //System.out.println("------------------------------------------------------------------------");

        
    }
    //controlla che il nome dell'utente passato come parametro sia presente negli utenti
    private boolean CheckUser(String NewUser)
    {
        synchronized(lock_user)
        {
            if(!this.UserState.keySet().contains(NewUser))return false;
            return true;
        }
    }
    //controlla che il progetto sia fra i progetti accessibili dall'utente loggato
    private boolean CheckPJT(String pjtName)
    {
        synchronized(lock_project)
        {
            if(!this.PJ_Chat.keySet().contains(pjtName)) return false;
            return true;
        }
    }
    private boolean CheckName(String name){return UserName.equals(name);}

    //mandoo il comando al server e stampo la risposta ricevuta
    private void generic_Command(String command, SocketChannel client) throws IOException {
        ByteBuffer msg = ByteBuffer.wrap(command.getBytes());
        client.write(msg);

        ByteBuffer reply = ByteBuffer.allocate(BUFFER_DIM);
        client.read(reply);

        String risposta = new String(reply.array()).trim();
        System.out.println("Ripostata ricevuta:");
        System.out.println(risposta);
    }

    //Comando per effettuare il login
    //se andato a buon fine registra anche l'interfaccia per le CallBacks e carica gli User-State e ProjectName-IP
    private void login_Command(String command, SocketChannel client)throws IOException {
        ByteBuffer msg = ByteBuffer.wrap(command.getBytes());
        client.write(msg);

        ByteBuffer reply = ByteBuffer.allocate(BUFFER_DIM);
        client.read(reply);

        String risposta = new String(reply.array()).trim();

        if(risposta.contains("Login effettuato correttamente."))
        {
            LOGGED=true;
            UserName=command.split(" ")[1];
            String[] split = risposta.split("#");

            ArrayList<String> list= new ArrayList<>(Arrays.asList(split));

            System.out.println(list.get(0));

            //System.out.println(list.get(1)); //contiene gli USERS-STATE
            if(list.size()>1)
            {
                checkUsers(list.get(1));
            }

            //System.out.println(list.get(2)); //contiene ProjectName-IP
            if(list.size() > 2)
            {
                checkProject(list.get(2));
            }

            serverRMI.registerForCallBacks(nei, command.split(" ")[1]);
        }
        else
            System.out.println(risposta);

        
    }
    //funzione chiamata da Login_Command per caricare gli utenti nel DB
    private void checkUsers(String users)
    {
        String[] split = users.split(" ");

        for (String string : split) {
            String[] US = string.split("-");
            this.UserState.put(US[0],US[1]);
        }
        //System.out.println(UserState);
    }
    //funzione chiamata da Login_Command per caricare i ProjectName-ChatThread nel DB 
    //e creare i thread delle chat
    private void checkProject(String pj)
    {
        try {
            String[] split = pj.split(" ");
            for (String string : split) {
                String[] US = string.split("&&");
                //MessagingQueue queue = new MessagingQueue();
                
                int port = Integer.parseInt(US[2]);
                MulticastSocket socket = new MulticastSocket(port);
                ChatThread ct = new ChatThread(socket, InetAddress.getByName(US[1]) , port );
                this.PJ_Chat.put(US[0],ct);
                //this.PJ_ChatQ.put(US[0],queue);

                //System.out.println("--------------");
                //System.out.println(this.PJ_Chat);
                //System.out.println(this.PJ_ChatQ);

                ct.start();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(PJip);
    }

    //effettuo il comando Logout se riuscito interrompe tutti i thread delle chat
    private void logout_Command(String command, SocketChannel client) throws IOException {

        serverRMI.unregisterForCallback(nei, UserName);

        
        ByteBuffer msg = ByteBuffer.wrap(command.getBytes());
        
        client.write(msg);

        ByteBuffer reply = ByteBuffer.allocate(BUFFER_DIM);
        client.read(reply);

        String risposta = new String(reply.array()).trim();

        System.out.println(risposta);

        LOGGED=false;
        if(risposta.contains("Logout Errato. "))
        {
            serverRMI.registerForCallBacks(nei, UserName);
            LOGGED=true;
        }
        else
        {
            //esco da tutte le chat una volta fatto il LOGOUT
            synchronized(lock_project)
            {
                for(String pjt : this.PJ_Chat.keySet())
                {
                    PJ_Chat.get(pjt).interrupt();
                    PJ_Chat.remove(pjt);
                    //PJ_ChatQ.remove(pjt);
                }
            }
        }

    }

    public void start() {
        try {
            Registry r = LocateRegistry.getRegistry(30000);
            this.serverRMI = (WorthServerRMI) r.lookup("SERVER");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (SocketChannel client = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(IP), PORT));) {

            while (true) {
                //leggo un nuovo comando
                String command =  getNewCommand();  
                
                //gestisco il comando
                gestCommand(command,client);
                if (EXIT)
                {
                    //se sono loggato quando esco faccio un logout
                    if(LOGGED)
                        logout_Command("logout "+UserName, client);
                    break;
                }
                command="";
            }
            
            client.close();
            scan.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws RemoteException {
        ClientMain cm = new ClientMain("127.0.0.1", 1999);
        cm.start();

        System.out.println("Bye Bye");
        System.exit(0);
    }
    
}
