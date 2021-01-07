package Tools;

import java.util.ArrayList;
import java.util.List;

public class MessagingQueue
{
    private List<String> queue;


    public MessagingQueue()
    {
        queue= new ArrayList<>();
    }

    public synchronized void put(String s)
    {
        queue.add(s);
    }
    
    //Restituisco una nuova lista uguale alla precedente, e pulisco quella vecchia
    public synchronized List<String> getAndClear() {
        List<String> old= new ArrayList<>(queue);
        queue.clear();
        return old; 
    }
}