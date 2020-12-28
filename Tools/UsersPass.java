package Tools;

public class UsersPass {
    private String Username;
    private String Password;

    public UsersPass(String User,String Pass)
    {
        this.Username=User;
        this.Password=Pass;
    }

    public UsersPass(){}

    public String getUser(){return this.Username;}
    public String getPassword(){return this.Password;}
    public void setUser(String Username){ this.Username=Username;}
    public void setPassword(String Password){ this.Password=Password;}
    
}
