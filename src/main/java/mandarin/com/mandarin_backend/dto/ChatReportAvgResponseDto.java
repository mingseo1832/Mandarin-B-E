package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mandarin.com.mandarin_backend.entity.ChatReportAvg;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReportAvgResponseDto {

    private Long chatReportAvgId;       // PK
    private Integer chatReportId;       // 연결된 ChatReport ID
    private Long userId;                // 유저 ID
    private Integer avgMandarinScore;   // 만다린 전체 평균 점수
    private String totalLabelKey;       // 라벨 키 (F1~F6)
    private Integer totalLabelScore;    // 해당 라벨 점수
    private LocalDateTime createdAt;    // 생성 시간

    /**
     * ChatReportAvg Entity → DTO 변환
     */
    public static ChatReportAvgResponseDto fromEntity(ChatReportAvg entity) {
        if (entity == null) {
            throw new IllegalArgumentException("ChatReportAvg entity cannot be null");
        }
        return ChatReportAvgResponseDto.builder()
                .chatReportAvgId(entity.getChatReportAvgId())
                .chatReportId(entity.getChatReport() != null ? entity.getChatReport().getChatReportId() : null)
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .avgMandarinScore(entity.getAvgMandarinScore())
                .totalLabelKey(entity.getTotalLabelKey() != null ? entity.getTotalLabelKey().name() : null)
                .totalLabelScore(entity.getTotalLabelScore())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

