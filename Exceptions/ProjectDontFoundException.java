package Exceptions;

public class ProjectDontFoundException extends RuntimeException {   
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ProjectDontFoundException()
    {
        super("Progetto non trovato");
    }
    public ProjectDontFoundException(String arg)
    {
        super("Progetto non trovato: "+arg);
    }
}
