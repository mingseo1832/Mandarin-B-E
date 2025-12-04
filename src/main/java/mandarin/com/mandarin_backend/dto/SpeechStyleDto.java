package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class SpeechStyleDto {
    @JsonProperty("politeness_level")
    private String politenessLevel;

    private String tone;

    @JsonProperty("common_endings")
    private List<String> commonEndings;

    @JsonProperty("frequent_interjections")
    private List<String> frequentInterjections;

    @JsonProperty("emoji_usage")
    private EmojiStyleDto emojiUsage;

    @JsonProperty("distinctive_habits")
    private List<String> distinctiveHabits;

    @JsonProperty("sample_sentences")
    private List<String> sampleSentences;
}
