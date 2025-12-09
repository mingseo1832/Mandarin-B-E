package mandarin.com.mandarin_backend.controller;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ApiResponse;
import mandarin.com.mandarin_backend.dto.SimulationResponseDto;
import mandarin.com.mandarin_backend.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    // 시뮬레이션 다건 조회 API
    // GET /simulation/character/{character_id}
    @GetMapping("/character/{character_id}")
    public ResponseEntity<ApiResponse<List<SimulationResponseDto>>> getSimulationsByCharacterId(
            @PathVariable("character_id") Long characterId) {

        ApiResponse<List<SimulationResponseDto>> response = 
                simulationService.getSimulationsByCharacterId(characterId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response); // 200 OK
        }

        return ResponseEntity.badRequest().body(response); // 실패 시 400
    }
}

