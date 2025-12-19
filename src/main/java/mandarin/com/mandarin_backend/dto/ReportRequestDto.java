package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDto {
    
    /**
     * 시뮬레이션 ID - 리포트를 저장할 시뮬레이션
     */
    private Long simulationId;
    
    private Long id; // User pk
}


