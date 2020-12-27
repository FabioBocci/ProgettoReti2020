package Exceptions;

public class NUserException extends RuntimeException
{

    private static final long serialVersionUID = 1L;

    public NUserException()
    {
        super("Utente gia usato");
    }
    public NUserException(String arg)
    {
        super("Utent gia usato:"+arg);
    }
}