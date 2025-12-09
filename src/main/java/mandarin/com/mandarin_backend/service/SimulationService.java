package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ApiResponse;
import mandarin.com.mandarin_backend.dto.SimulationResponseDto;
import mandarin.com.mandarin_backend.entity.Simulation;
import mandarin.com.mandarin_backend.repository.SimulationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationRepository simulationRepository;

    // character_id로 시뮬레이션 다건 조회
    public ApiResponse<List<SimulationResponseDto>> getSimulationsByCharacterId(Long characterId) {

        List<Simulation> simulations = simulationRepository.findByCharacterCharacterId(characterId);

        if (simulations.isEmpty()) {
            return ApiResponse.success("해당 캐릭터의 시뮬레이션이 없습니다.", List.of());
        }

        // Simulation 엔티티를 SimulationResponseDto로 변환
        List<SimulationResponseDto> responseDtos = simulations.stream()
                .map(simulation -> SimulationResponseDto.builder()
                        .simulationId(simulation.getSimulationId())
                        .characterId(simulation.getCharacter().getCharacterId())
                        .startTime(simulation.getStartTime())
                        .endTime(simulation.getEndTime())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success("시뮬레이션 정보 조회 성공", responseDtos);
    }
}

