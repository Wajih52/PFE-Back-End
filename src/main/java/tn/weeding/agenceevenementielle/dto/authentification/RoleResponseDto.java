package tn.weeding.agenceevenementielle.dto.authentification;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class RoleResponseDto {
    private Long id;
    private String nom;
    private String description;
    private LocalDateTime creationDate;
    private LocalDateTime modificationDate;
    private Boolean active;

}
