package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserPersonaDto {
    private String name;

    @JsonProperty("speech_style")
    private SpeechStyleDto speechStyle;
}
