package mandarin.com.mandarin_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatReportCreateRequestDto {
    private Long simulation_id; // Simulation PK
    private Long id;            // User PK
}
