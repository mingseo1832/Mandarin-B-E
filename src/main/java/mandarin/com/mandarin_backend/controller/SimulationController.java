package mandarin.com.mandarin_backend.controller;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ApiResponse;
import mandarin.com.mandarin_backend.dto.SimulationMessageRequestDto;
import mandarin.com.mandarin_backend.dto.SimulationMessageResponseDto;
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

    /**
     * 시뮬레이션 대화 다건 조회 API
     * GET /simulation/message/{simulation_id}
     * 
     * simulation_id에 해당하는 시뮬레이션의 모든 대화 정보들을 반환합니다.
     */
    @GetMapping("/message/{simulation_id}")
    public ResponseEntity<ApiResponse<List<SimulationMessageResponseDto>>> getMessagesBySimulationId(
            @PathVariable("simulation_id") Long simulationId) {

        ApiResponse<List<SimulationMessageResponseDto>> response = 
                simulationService.getMessagesBySimulationId(simulationId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response); // 200 OK
        }

        return ResponseEntity.badRequest().body(response); // 실패 시 400
    }

    /**
     * 시뮬레이션 대화 저장 API
     * POST /simulation/message
     * 
     * 사용자 메시지를 저장하고 AI 응답을 받아 반환합니다.
     */
    @PostMapping("/message")
    public ResponseEntity<ApiResponse<SimulationMessageResponseDto>> sendMessage(
            @RequestBody SimulationMessageRequestDto request) {

        ApiResponse<SimulationMessageResponseDto> response = simulationService.sendMessage(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response); // 200 OK
        }

        return ResponseEntity.badRequest().body(response); // 실패 시 400
    }
}

