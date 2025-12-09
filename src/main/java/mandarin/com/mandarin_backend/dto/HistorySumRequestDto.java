package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 히스토리 요약 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorySumRequestDto {
    
    /**
     * 캐릭터 ID (UserCharacter PK)
     */
    private Long characterId;
    
    /**
     * 요약할 히스토리 내용
     */
    private String history;
}
