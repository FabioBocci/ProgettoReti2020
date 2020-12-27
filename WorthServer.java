import java.util.List;
import Exceptions.*;

public interface WorthServer{

    public boolean Login(String Username, String Password)throws IllegalArgumentException,UserDontFoundException;
    public boolean Logout(String User)throws IllegalArgumentException,UserDontFoundException;
    public boolean CreateProject(String PJTname, String User)throws IllegalArgumentException,UserDontFoundException,ProjectNameAlreadyUsed;
    public boolean EndProject(String PJTname, String User)throws IllegalArgumentException,UserDontFoundException,ProjectDontFoundException;
    public List<String> ListProject();

    public boolean addMembers(String PJTname, String newUser)throws IllegalArgumentException,UserDontFoundException,ProjectDontFoundException;
    public List<String> ShowMembers(String PJTname)throws ProjectDontFoundException;
    
    public List<List<Card>> ShowCards(String PJTname) throws ProjectDontFoundException;
    public Card ShowCard(String PJTname,String CARDname)throws ProjectDontFoundException,CardDontFoundException;
    public boolean MoveCard(String PJTname, String CARDname, String lstOLD, String lstNEW)throws ProjectDontFoundException,CardDontFoundException,IllegalMoveException;
    public List<String> GetCardHistory(String PJTname, String CARDname)throws ProjectDontFoundException,CardDontFoundException;
    
}



