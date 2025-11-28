package mandarin.com.mandarin_backend.controller;

import mandarin.com.mandarin_backend.dto.AnalyzeRequestDto;
import mandarin.com.mandarin_backend.dto.UserPersonaDto;
import mandarin.com.mandarin_backend.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/persona")
@RequiredArgsConstructor
public class PersonaController {

    private final AnalysisService analysisService;

    // 앱에서 요청 보내는 주소: POST http://localhost:8080/api/persona/analyze
    @PostMapping("/analyze")
    public ResponseEntity<UserPersonaDto> analyze(@RequestBody AnalyzeRequestDto request) {
        
        System.out.println("앱에서 요청 옴: " + request.getTargetName());

        UserPersonaDto result = analysisService.analyzePersona(
            request.getTextContent(), 
            request.getTargetName()
        );

        return ResponseEntity.ok(result);
    }
}