package mandarin.com.mandarin_backend.controller;

import mandarin.com.mandarin_backend.dto.AnalyzeRequestDto;
import mandarin.com.mandarin_backend.dto.ParseInfoResponseDto;
import mandarin.com.mandarin_backend.dto.UserPersonaDto;
import mandarin.com.mandarin_backend.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/persona")
@RequiredArgsConstructor
public class PersonaController {

    private final AnalysisService analysisService;

    /**
     * JSON 요청으로 페르소나 분석
     * POST /api/persona/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<UserPersonaDto> analyze(@RequestBody AnalyzeRequestDto request) {
        
        System.out.println("[Analyze] JSON 요청 - 대상: " + request.getTargetName());

        UserPersonaDto result = analysisService.analyzePersona(
            request.getTextContent(), 
            request.getTargetName()
        );

        return ResponseEntity.ok(result);
    }

    /**
     * 카카오톡 텍스트 파일 업로드로 페르소나 분석
     * POST /api/persona/analyze/file
     * 
     * @param file 카카오톡 대화 내보내기 텍스트 파일 (.txt)
     * @param targetName 분석 대상 인물 이름
     * @return 추출된 페르소나 정보
     */
    @PostMapping("/analyze/file")
    public ResponseEntity<UserPersonaDto> analyzeFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetName") String targetName) {
        
        System.out.println("[Analyze] 파일 업로드 - 파일명: " + file.getOriginalFilename() + ", 대상: " + targetName);

        // 파일 유효성 검사
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        try {
            // 파일 내용을 문자열로 변환 (UTF-8 인코딩)
            String textContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            
            // Python 서버로 분석 요청
            UserPersonaDto result = analysisService.analyzePersona(textContent, targetName);
            
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + e.getMessage());
        }
    }

    /**
     * 카카오톡 텍스트 파일 파싱 정보 조회
     * POST /api/persona/parse-info
     * 
     * @param file 카카오톡 대화 내보내기 텍스트 파일 (.txt)
     * @return 파싱 정보 (참여자 목록, 대화 기간, 메시지 수 등)
     */
    @PostMapping("/parse-info")
    public ResponseEntity<ParseInfoResponseDto> parseInfo(@RequestParam("file") MultipartFile file) {
        
        System.out.println("[ParseInfo] 파일 업로드 - 파일명: " + file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        try {
            String textContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            
            ParseInfoResponseDto result = analysisService.parseInfo(textContent);
            
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + e.getMessage());
        }
    }
}