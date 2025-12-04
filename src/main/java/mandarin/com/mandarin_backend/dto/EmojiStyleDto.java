package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EmojiStyleDto {
    private String frequency;

    @JsonProperty("preferred_type")
    private String preferredType;

    @JsonProperty("laugh_sound")
    private String laughSound;
}
