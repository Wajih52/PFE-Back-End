package tn.weeding.agenceevenementielle.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResponse {
    @JsonProperty("titre")
    private String titre;

    @JsonProperty("message")
    private String message;

    @JsonProperty("code")
    private int code;

}
