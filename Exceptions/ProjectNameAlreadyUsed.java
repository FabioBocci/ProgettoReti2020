package Exceptions;

public class ProjectNameAlreadyUsed extends RuntimeException{

    private static final long serialVersionUID = 1L;
    public ProjectNameAlreadyUsed(String arg)
    {
        super("Nome progetto gia usato:"+arg);
    }
    public ProjectNameAlreadyUsed()
    {
        super("Nome progetto gia usato");
    }
    
}
