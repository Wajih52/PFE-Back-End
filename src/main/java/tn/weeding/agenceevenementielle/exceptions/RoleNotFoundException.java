package tn.weeding.agenceevenementielle.exceptions;

public class RoleNotFoundException extends RuntimeException{
    public RoleNotFoundException(Long id){
        super("Role avec id " + id + " n'existe pas !");
    }
}
