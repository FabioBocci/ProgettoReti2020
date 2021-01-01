package Tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ChatThread extends Thread {

    final static int BUFFER_SIZE = 4096;
    private MessagingQueue queue;


    private int PORT;
    private MulticastSocket multicast;
    private InetAddress group;

    public ChatThread(MessagingQueue queue, MulticastSocket socket , InetAddress group , int port) throws IOException
    {
        this.queue=queue;
        this.PORT=port;
        this.multicast = socket;
        this.group = group;
    }

    public void sendMsg(String msg)throws NullPointerException, IOException
    {
        if(msg == null) throw new NullPointerException();
        byte[] buffer = msg.getBytes();
        DatagramPacket datagram = new DatagramPacket(buffer,buffer.length,this.group,this.PORT);
        multicast.send(datagram);
    }
    
    public void addMessage(String msg)
    {
        queue.put(msg);
    }
    
    public void recive() throws IOException
    {
        byte[] msg_recived= new byte[BUFFER_SIZE];
        DatagramPacket PACK = new DatagramPacket(msg_recived, msg_recived.length ,this.group,this.PORT);
        multicast.receive(PACK);
        String msg = new String(PACK.getData(),0,PACK.getLength(),"UTF-8");
        
        addMessage(msg);
    }
    @Override
    public void run() {
        try {
            multicast.joinGroup(group);
            while(!Thread.interrupted())
            {
                recive();
            }
            multicast.leaveGroup(group);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        multicast.close();
    }
    
}
