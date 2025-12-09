package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 히스토리 요약 응답 DTO (Python 서버로부터의 응답)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorySumResponseDto {
    
    /**
     * 요약된 히스토리
     */
    private String summary;
}
