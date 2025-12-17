package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationMessageResponseDto {

    private String sender;  // "user" = 사용자, "assistant" = AI (캐릭터)

    private String content;

    private LocalDateTime timestamp;
}

