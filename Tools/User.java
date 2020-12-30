package Tools;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {
    private String Username;
    private String Password;

    public User(String User, String Pass)
    {
        this.Username=User;
        this.Password=Pass;
        this.online=false;
    }

    public User(){}

    public String getUser(){return this.Username;}
    public String getPassword(){return this.Password;}
    public void setUser(String Username){ this.Username=Username;}
    public void setPassword(String Password){ this.Password=Password;}


    private boolean online;
    private NotifyEventInterface nei;

    @JsonIgnore
    public void setOnline(boolean bool){this.online=bool;}
    @JsonIgnore
    public void setNEI(NotifyEventInterface n){this.nei=n;}
    @JsonIgnore
    public NotifyEventInterface getNEI(){return this.nei;}
    @JsonIgnore
    public boolean isOnline(){return this.online;}
    
}
