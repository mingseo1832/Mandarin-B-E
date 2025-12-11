package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mandarin.com.mandarin_backend.entity.ReportCharacter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCharacterResponseDto {

    private Integer reportCharacterId;  // PK
    private Long characterId;           // 캐릭터 ID
    private String conflictName;        // 갈등 요소 이름
    private Integer dangerLevel;        // 위험도 (0~100)
    private String description;         // 갈등에 대한 설명
    private String solution;            // 해결 방안

    /**
     * ReportCharacter Entity → DTO 변환
     */
    public static ReportCharacterResponseDto fromEntity(ReportCharacter entity) {
        if (entity == null) {
            throw new IllegalArgumentException("ReportCharacter entity cannot be null");
        }
        return ReportCharacterResponseDto.builder()
                .reportCharacterId(entity.getReportCharacterId())
                .characterId(entity.getCharacter() != null ? entity.getCharacter().getCharacterId() : null)
                .conflictName(entity.getConflictName())
                .dangerLevel(entity.getDangerLevel())
                .description(entity.getDescription())
                .solution(entity.getSolution())
                .build();
    }
}

