package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {
    
    /**
     * 시뮬레이션 ID - 이 ID로 Simulation과 UserCharacter 정보를 조회하여
     * AI에게 컨텍스트 정보를 전달합니다.
     */
    @JsonProperty("simulationId")
    private Long simulationId;
    
    @JsonProperty("userMessage")
    private String userMessage;
    
    @Builder.Default
    private List<ChatLogDto> history = new ArrayList<>();
}


