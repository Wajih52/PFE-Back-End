package tn.weeding.agenceevenementielle.dto.authentification;

import lombok.Data;

@Data
public class ActiverCompteDto {
    private String token;
    private String nouveauMotDePasse;

}
