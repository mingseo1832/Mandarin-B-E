package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationMessageRequestDto {

    @JsonProperty("simulation_id")
    private Long simulationId;

    private Boolean sender;  // false = 사용자, true = AI

    private String content;
}

