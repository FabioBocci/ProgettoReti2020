package Tools;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import Exceptions.*;

public class Project {

    private String ID_NAME;
    private List<Card> Cards;
    
    private List<String> Cards_TODO; 
    private List<String> Cards_InProgess;
    private List<String> Cards_Revisited; 
    private List<String> Cards_Done;
    
    private List<String> Membri;

    private String IP_Multicast;
    private int PORT = 1998;
    private boolean DELETED;
    private String ABS_path;

    private ObjectMapper OM;

    public Project(String ABS_PATH, String NomeProgetto , String ip, int port) throws IOException {
        DELETED=false;
        this.ID_NAME = NomeProgetto;
        this.Cards = new ArrayList<>();
        
        this.Cards_TODO = new ArrayList<>(); 
        this.Cards_InProgess = new ArrayList<>(); 
        this.Cards_Revisited = new ArrayList<>(); 
        this.Cards_Done = new ArrayList<>();
        
        this.Membri = new ArrayList<>();

        this.ABS_path = ABS_PATH;
        this.OM = new ObjectMapper();

        this.IP_Multicast=ip;
        this.PORT=port;

        //Aggiungere creaizone della Cartella
        boolean found = false;
        File f = new File(ABS_path);
        if (!f.isDirectory())
            throw new RuntimeException("Path not a direcotory"); // TODO creare una nuova eccezione
                                                                 // PathNotDirecotoryException
        for (String filename : f.list()) {
            if (filename == ID_NAME) {
                found = true;
                break;
            }
        }
        Path path = Paths.get(ABS_path + "/" + ID_NAME + "/");
        if (!found) {
            Files.createDirectories(path);
        }
        checkCards();
    }

    public String getIP(){return this.IP_Multicast;}
    public int getPORT(){return this.PORT;}
    public void setIP(String ip){this.IP_Multicast=ip;}
    public void setPORT(int port){this.PORT=port;}

    public void checkCards() throws IOException {
        Path path;
        path = Paths.get(ABS_path + "/" + ID_NAME + "/");
        

        Card c= null;
        File files = path.toFile();
        for (File fn : files.listFiles()) {
            if (fn.getName().contains("Users.txt"))
                setUsers(fn);
            else {
                if (fn.toString().contains(".json"))
                    {
                        c= OM.readValue(fn, Card.class);
                        Cards.add(c);
                        switch(c.getState())
                        {
                            case "TODO":
                                Cards_TODO.add(c.getName());
                                break;
                            case "INPROGRESS":
                                Cards_InProgess.add(c.getName());
                                break;
                            case "TOBEREVISED":
                                Cards_Revisited.add(c.getName());
                                break;
                            case "DONE":
                                Cards_Done.add(c.getName());
                                break;
                        }
                    }
            }
        }
    }

    public String getName(){return this.ID_NAME;}
    private void setUsers(File f) throws FileNotFoundException {
        Scanner sc = new Scanner(f);
        while (sc.hasNextLine()) {
            Membri.add((sc.nextLine()));
        }
        sc.close();
    }

    public void SaveAll() throws IOException 
    {
        if(!DELETED)    //controllo che il progetto non sia eliminato
        {
            Path path = Paths.get(ABS_path + "/" + ID_NAME + "/");
            for (Card c : Cards) {
                File f = new File(path.toString()+ "/" + c.getName() + ".json");
                System.out.println(f.toString());
                try {
                    OM.writeValue(f, c);
                } catch (JsonGenerationException e) { 
                    e.printStackTrace();
                } catch (JsonMappingException e) {
                    e.printStackTrace();
                }
                
            }
    
            // Salvataggiu degli Membri in un File txt
            try {
                FileOutputStream outputStream = new FileOutputStream(new File(path.toString()+ "/" + "Users.txt"));
    
                for(String str : Membri)
                {
                    String temp=str+"\n";
                    outputStream.write(temp.getBytes());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            
    
            System.out.println("Tutti i file Salvati");
        }
    }

    

    //Aggiunge un Nuovo membro al progetto. se gia presente lancia una eccezione
    public void AddMember(String User)
    {
        if(!DELETED)
        {
            if(this.Membri.contains(User))throw new IllegalArgumentException();         //da modificare con una eccezione IllegalUserException
            Membri.add(User);
        }
    }
    
    //return true se il membro è gia presente false altrimenti
    public boolean IsMember(String User){if(!DELETED)return Membri.contains(User); return false;}

    //Crea una nuova Card e la inserisce, se il nome non è gia usato da un'altra Card
    public void AddCard(String Name, String Desc)
    {
        if(!DELETED)
        {
            for(Card c : Cards)
            {
                if(c.getName()==Name)throw new IllegalArgumentException();
            }
            Card s = new Card(Name, Desc);
            Cards.add(s);
            Cards_TODO.add(Name);
        }
    }
    //Muove gli stati di una Card
    public void MoveCard(String Name, String OldState, String NewState)throws IllegalArgumentException, IllegalMoveException
    {
        if(DELETED) return;
        Card s = null;
        for(Card c : Cards)
        {
            if(c.getName().equals(Name))
            {
                s=c;
            }
        }

        if(s==null) throw new CardDontFoundException(Name);

        if( ! s.getState().toLowerCase().equals(OldState.toLowerCase())) throw new IllegalArgumentException("Lo stato vecchio non combacia");

        switch(OldState.toUpperCase())
        {
            case "TODO":
                if(NewState.toUpperCase().equals( "INPROGRESS")) {throw new IllegalMoveException(OldState,NewState);}
                else{
                    s.ChangeStatus("INPROGRESS");
                    Cards_TODO.remove(Name);
                    Cards_InProgess.add(Name);
                    break;
                }
            case "INPROGRESS":
                if(NewState.toUpperCase().equals("TOBEREVISED") && NewState.toUpperCase().equals("DONE")) {throw new IllegalMoveException(OldState,NewState);}
                else{
                    s.ChangeStatus(NewState.toUpperCase());
                    if(NewState.toUpperCase() == "TOBEREVISED")
                    {
                        Cards_InProgess.remove(Name);
                        Cards_Revisited.add(Name);
                    }
                    else{
                        Cards_InProgess.remove(Name);
                        Cards_Done.add(Name);
                    }
                    break;
                }
            case "TOBEREVISED":
            if(NewState.toUpperCase().equals( "INPROGRESS") && NewState.toUpperCase().equals( "DONE")) {throw new IllegalMoveException(OldState,NewState);}
                else{
                    s.ChangeStatus(NewState.toUpperCase());
                    if(NewState.toUpperCase().equals("INPROGRESS"))
                    {
                        Cards_Revisited.remove(Name);
                        Cards_InProgess.add(Name);
                    }
                    else{
                        Cards_Revisited.remove(Name);
                        Cards_Done.add(Name);
                    }
                    break;
                }
            default:
                throw new IllegalArgumentException("Movimento non riconosciuto");
        }
    }

    //restituisce una lista con i nomi di tutte le Card
    public List<String> GetCards()
    {
        if(DELETED)return null;
        List<String> lst = new ArrayList<>();
        lst.addAll(Cards_TODO);
        lst.addAll(Cards_InProgess);
        lst.addAll(Cards_Revisited);
        lst.addAll(Cards_Done);
        return lst;
    }

    public boolean IsDone(){if(DELETED) return true;return Cards_Done.size()==Cards.size();}

    public List<String> getCardHistory(String name)
    {
        if(DELETED)return null;
        Card c=null;
        for(Card s : Cards)
        {
            if(s.getName().equals(name)) c=s;
        }
        if(c==null)return null;
        return c.getCardHistory();
    }

    public List<String> GetCardInfo(String name)
    {
        if(DELETED)return null;
        Card c=null;
        for(Card s : Cards)
        {
            if(s.getName().equals(name)) c=s;
        }
        if(c==null)return null;
        return c.getInfo();
    }
    public List<String> GetMember(){if(DELETED)return null; return new ArrayList<>(this.Membri);}

    public boolean delete()
    {
        if(DELETED)return true;
        Path path = Paths.get(ABS_path + "/" + ID_NAME + "/");
        File f = path.toFile();

        if(f.isDirectory())
        {
            for(File fn : f.listFiles())
            {
                fn.delete();
            }
        }
        DELETED=true;
        return f.delete();
    }

    //--------------------------TEST-----------------------//
    public static void main(String[] args) throws IOException {
        Project pj = new Project("C:/Users/Fabio/Desktop/Progetto Reti Worth/Projects", "03-Prova2","239.0.0.5",1998);
        pj.AddCard("ROBA", "prova di una lunga descirizone");
        System.out.println(pj.GetCards());
        //pj.MoveCard("ROBA2", "TODO", "INPROGRESS");
        //System.out.println(pj.Cards.get(0).getInfo());
        System.out.println(pj.GetCardInfo("ROBA"));
        System.out.println(pj.getCardHistory("ROBA"));

        System.out.println(pj.GetMember());
        //pj.AddMember("Fabio");
        //pj.AddMember("Luca");
        pj.delete();
        pj.SaveAll();
    }
}
