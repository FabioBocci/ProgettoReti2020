package Tools;

import java.util.ArrayList;
import java.util.List;

public class MessagingQueue
{
    private List<String> queue;
    private String msg;
    private boolean sended;

    public MessagingQueue()
    {
        queue= new ArrayList<>();
    }

    public synchronized void put(String s)
    {
        queue.add(s);
    }

    public synchronized List<String> getAndClear() {
        List<String> old= new ArrayList<>(queue);
        queue.clear();
        return old; 
    }

    public synchronized void sendMsg(String msg)
    {
        sended=false;
        this.msg=msg;
    }
    public synchronized String getMsg() {
        if(!sended)
        {
            sended=true;
            return this.msg;
        }
        return "";
    }

    public synchronized boolean isSended(){return sended;}
}