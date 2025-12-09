package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import mandarin.com.mandarin_backend.entity.enums.SimulationPurpose;
import mandarin.com.mandarin_backend.entity.enums.SimulationCategory;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationResponseDto {
    private Long simulationId;
    private Long characterId;
    private String simulationName;
    private SimulationPurpose purpose;
    private SimulationCategory category;
    private LocalDateTime time;
    private LocalDateTime lastUpdateTime;
    private Boolean isFinished;
}

