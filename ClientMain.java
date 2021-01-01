import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import Exceptions.NUserException;
import Tools.*;

public class ClientMain extends RemoteObject implements NotifyEventInterface, ClientInterface {

    // private static final String EXIT_CMD = "exit";
    private static final long serialVersionUID = 1L;
    private static final int BUFFER_DIM = 4096;

    private String IP = "127.0.0.1";
    private int PORT = 1999;

    private boolean EXIT = false;
    private Map<String, String> UserState;
    private Map<String, String> PJip;
    private WorthServerRMI serverRMI;
    private NotifyEventInterface nei;
    private Scanner scan;

    private String UserName;
    private Boolean LOGGED;

    public ClientMain(String ip, int Port) throws RemoteException {
        super();
        this.IP = ip;
        this.PORT = Port;
        UserState = new HashMap<>();
        this.scan = new Scanner(System.in);
        LOGGED = false;
        nei = (NotifyEventInterface) UnicastRemoteObject.exportObject(this, 0);

        PJip = new HashMap<>();
    }

    public synchronized void login(String User, String Password) throws RemoteException {

        // altro codice

        // ricezione degli utenti online ed ofline
        NotifyEventInterface callbackObj = (NotifyEventInterface) UnicastRemoteObject.exportObject(this, 0);
        serverRMI.registerForCallBacks(callbackObj, User);
    }

    @Override
    public synchronized void notifyEventUser(String User, String Status) throws RemoteException {
        UserState.put(User, Status);
    }

    @Override
    public synchronized void notifyEventChat(String IP, int PORT) throws RemoteException {
        // TODO Aggiungere un nuovo thread per la nuova chat
    }

    @Override
    public synchronized void notifyEventProjectCancel(String address, int port) throws RemoteException {
        // TODO eliminare un thread

    }

    private String getNewCommand() {
        return scan.nextLine();
    }

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
                    logout_Command(Command, client);
                else
                    System.out.println("User NOT Logged");
                break;
            case "createproject":
                if (LOGGED)
                    generic_Command(Command, client);
                else
                    System.out.println("User NOT Logged");
                break;
            case "endproject":
                if (LOGGED)
                    generic_Command(Command, client);
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
                    generic_Command(Command, client);
                else
                    System.out.println("User NOT Logged");
                break;
            case "showmember":
                if (LOGGED)
                    generic_Command(Command, client);
                else
                    System.out.println("User NOT Logged");
                break;
            case "showcards":
                if (LOGGED)
                    generic_Command(Command, client);
                else
                    System.out.println("User NOT Logged");
                break;
            case "showcard":
                if (LOGGED)
                    generic_Command(Command, client);
                else
                    System.out.println("User NOT Logged");
                break;
            case "getcardhistory":
                if (LOGGED)
                    generic_Command(Command, client);
                else
                    System.out.println("User NOT Logged");
                break;
            case "movecard":
                if (LOGGED)
                    generic_Command(Command, client);
                else
                    System.out.println("User NOT Logged");
                break;
            case "exit":
                EXIT = true;
                break;
            case "help":
                help();
                break;

            case "listuser":
                if (LOGGED)
                    System.out.print(UserState.keySet());
                else
                    System.out.println("User NOT Logged");
                break;

            case "listuseronline":
                for (String use : UserState.keySet()) {
                    if (UserState.get(use).contains("ONLINE"))
                        System.out.print(use + " ");
                }
                System.out.println("");
                break;
            default:
                System.out.println("Comando non riconosciuto");
        }
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
        System.out.flush();
        
    }

    private void generic_Command(String command, SocketChannel client) throws IOException {
        ByteBuffer msg = ByteBuffer.wrap(command.getBytes());
        client.write(msg);

        ByteBuffer reply = ByteBuffer.allocate(BUFFER_DIM);
        client.read(reply);

        String risposta = new String(reply.array()).trim();
        System.out.println("Ripostata ricevuta:");
        System.out.println(risposta);
    }

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
    private void checkUsers(String users)
    {
        String[] split = users.split(" ");

        for (String string : split) {
            String[] US = string.split("-");
            this.UserState.put(US[0],US[1]);
        }
        //System.out.println(UserState);
    }

    private void checkProject(String pj)
    {
        String[] split = pj.split(" ");
        for (String string : split) {
            String[] US = string.split("&&");
            this.PJip.put(US[0],US[1]);
        }
        //System.out.println(PJip);
    }

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
                    break;
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

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
}
