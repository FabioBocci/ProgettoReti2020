import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import Tools.*;

public class ClientMain extends RemoteObject implements NotifyEventInterface, ClientInterface {

    //private static final String EXIT_CMD = "exit";
    private static final long serialVersionUID = 1L;
    private static final int BUFFER_DIM = 4096;

    private String IP = "127.0.0.1";
    private int PORT = 1999;

    private boolean EXIT = false;
    private Map<String, String> UserState;
    private WorthServerRMI serverRMI;
    private Scanner scan; 
    public ClientMain(String ip, int Port) {
        super();
        this.IP = ip;
        this.PORT = Port;
        UserState = new HashMap<>();
        this.scan =  new Scanner(System.in);

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

    public void start() {
        try {
            Registry r = LocateRegistry.getRegistry(30000);
            this.serverRMI = (WorthServerRMI) r.lookup("SERVER");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (SocketChannel client = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(IP), PORT));) {

            while (true) {
                String command =  getNewCommand();
                

                if (EXIT)
                    break;

                ByteBuffer msg = ByteBuffer.wrap(command.getBytes());
                client.write(msg);

                ByteBuffer reply = ByteBuffer.allocate(BUFFER_DIM);
                client.read(reply);
                System.out.println(reply.toString());
                String risposta = new String(reply.array()).trim();

                System.out.println("Ripostata ricevuta:");
                System.out.println(risposta);

            }
            
            client.close();
            scan.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
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
