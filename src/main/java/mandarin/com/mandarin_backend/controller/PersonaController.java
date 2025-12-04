package mandarin.com.mandarin_backend.controller;

import mandarin.com.mandarin_backend.dto.AnalyzeRequestDto;
import mandarin.com.mandarin_backend.dto.ParseInfoResponseDto;
import mandarin.com.mandarin_backend.dto.UserPersonaDto;
import mandarin.com.mandarin_backend.service.AnalysisService;
import mandarin.com.mandarin_backend.util.PiiMaskingUtil;

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
     * 
     * @param request textContent, targetName, periodDays, startDate, endDate, bufferDays
     */
    @PostMapping("/analyze")
    public ResponseEntity<UserPersonaDto> analyze(@RequestBody AnalyzeRequestDto request) {

        String safeText = PiiMaskingUtil.mask(request.getTextContent()); // 여기서 마스킹 적용
        
        System.out.println("[Analyze] JSON 요청 - 대상: " + request.getTargetName() 
            + ", 기간: " + (request.getStartDate() != null ? 
                request.getStartDate() + "~" + request.getEndDate() : 
                request.getPeriodDays() + "일"));

        UserPersonaDto result = analysisService.analyzePersona(
            safeText, // 마스킹 된 텍스트
            request.getTargetName(),
            request.getPeriodDays(),
            request.getStartDate(),
            request.getEndDate(),
            request.getBufferDays()
        );

        return ResponseEntity.ok(result);
    }

    /**
     * 카카오톡 텍스트 파일 업로드로 페르소나 분석
     * POST /api/persona/analyze/file
     * 
     * @param file 카카오톡 대화 내보내기 텍스트 파일 (.txt)
     * @param targetName 분석 대상 인물 이름
     * @param periodDays 분석할 기간 - 최근 N일 (기본값 14)
     * @param startDate 시작일 (YYYY-MM-DD, endDate와 함께 사용 시 periodDays보다 우선)
     * @param endDate 종료일 (YYYY-MM-DD)
     * @param bufferDays 시작일 이전 버퍼 일수 (기본값 7)
     * @return 추출된 페르소나 정보
     */
    @PostMapping("/analyze/file")
    public ResponseEntity<UserPersonaDto> analyzeFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetName") String targetName,
            @RequestParam(value = "periodDays", required = false, defaultValue = "14") Integer periodDays,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "bufferDays", required = false, defaultValue = "7") Integer bufferDays) {
        
        System.out.println("[Analyze] 파일 업로드 - 파일명: " + file.getOriginalFilename() 
            + ", 대상: " + targetName
            + ", 기간: " + (startDate != null ? startDate + "~" + endDate : periodDays + "일"));

        // 파일 유효성 검사
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        try {
            // 파일 내용을 문자열로 변환 (UTF-8 인코딩)
            String rawTextContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 마스킹 적용
            String safeTextContent = PiiMaskingUtil.mask(rawTextContent);
            
            // Python 서버로 분석 요청 (마스킹 데이터 전달)
            UserPersonaDto result = analysisService.analyzePersona(
                safeTextContent, targetName, periodDays, startDate, endDate, bufferDays);
            
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
            String rawTextContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            String safeTextContent = PiiMaskingUtil.mask(rawTextContent); // 여기서도 마스킹
            
            ParseInfoResponseDto result = analysisService.parseInfo(safeTextContent);
            
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + e.getMessage());
        }
    }
}