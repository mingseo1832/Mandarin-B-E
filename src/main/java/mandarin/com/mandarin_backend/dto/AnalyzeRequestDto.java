package mandarin.com.mandarin_backend.dto;

import lombok.Data;

@Data
public class AnalyzeRequestDto {
    private String textContent;
    private String targetName;
}
