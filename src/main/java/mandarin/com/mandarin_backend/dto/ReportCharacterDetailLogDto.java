package mandarin.com.mandarin_backend.dto;

import lombok.*;
import mandarin.com.mandarin_backend.entity.ReportCharacterDetailLog;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCharacterDetailLogDto {

    private String sender;          // USER 또는 CHARACTER
    private String content;         // messageKakao
    private LocalDateTime timestamp;

    public static ReportCharacterDetailLogDto fromEntity(ReportCharacterDetailLog entity) {
        if (entity == null) {
            throw new IllegalArgumentException("ReportCharacterDetailLog entity cannot be null");
        }
        // sender: "user" = 사용자, "assistant" = AI (캐릭터)
        return ReportCharacterDetailLogDto.builder()
                .sender(entity.getSender())
                .content(entity.getMessageKakao())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
