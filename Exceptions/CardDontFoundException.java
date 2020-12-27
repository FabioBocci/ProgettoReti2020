package Exceptions;

public class CardDontFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CardDontFoundException()
    {
        super("Carta non trovata");
    }
    public CardDontFoundException(String arg)
    {
        super("Carta non trovata: "+arg);
    }
    
}
