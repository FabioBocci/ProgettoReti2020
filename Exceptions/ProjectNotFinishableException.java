package Exceptions;

public class ProjectNotFinishableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProjectNotFinishableException(String s)
    {super(s);}
    public ProjectNotFinishableException()
    {
        
    }
    
}
