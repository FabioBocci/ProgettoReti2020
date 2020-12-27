package Exceptions;

public class UserDontFoundException extends RuntimeException{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public UserDontFoundException()
    {
        super("Utente non trovato");
    }

    public UserDontFoundException(String arg)
    {
        super("Utente non trovato: "+arg);
    }
    
}
