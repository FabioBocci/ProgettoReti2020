import Exceptions.*;

public interface WorthServer{

    public boolean Login(String Username, String Password)throws IllegalArgumentException,UserDontFoundException;

    
}



