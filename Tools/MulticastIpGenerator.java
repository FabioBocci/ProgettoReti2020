package Tools;
import java.util.Random;
import java.util.ArrayList;

public class MulticastIpGenerator {
    private ArrayList<String> IP_lst;
    private ArrayList port;

    private static final int MAX_GEN = 100;
    private int position=0;
    public MulticastIpGenerator()
    {
        IP_lst= new ArrayList<>();
        port= new ArrayList<>();
        generate();
    }
    private void generate()
    {
        int i=0;
        Random random = new Random();
        boolean ok;
        String newIp;
        while(i<100)
        {
            newIp="";
            ok =false;
            while(!ok)
            {
                newIp=newIp+((int) (random.nextFloat()*14)+224)+"."+((int) (random.nextFloat()*254)+1)+"."+((int) (random.nextFloat()*254)+1)+"."+((int) (random.nextFloat()*254)+1);
                if(!IP_lst.contains(newIp)) ok=true;
            }
            IP_lst.add(newIp);
            i++;
        }

    }

    public String nextIP(){
        position++;
        if(position>=MAX_GEN) return null;
        return IP_lst.get(position-1);}
    public int nextPORT(){return 1998;}


    public static void main(String[] args) {
        MulticastIpGenerator mui = new MulticastIpGenerator();

        for(int i=0; i<30;i++)
        {
            System.out.println("IP:"+mui.nextIP()+"  PORT:"+mui.nextPORT());
        }
        System.out.println(mui.position);
    }
}
