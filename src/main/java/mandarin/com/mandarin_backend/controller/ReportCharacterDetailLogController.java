package mandarin.com.mandarin_backend.controller;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ReportCharacterDetailLogDto;
import mandarin.com.mandarin_backend.service.ReportCharacterDetailLogService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/report/character")
public class ReportCharacterDetailLogController {

    private final ReportCharacterDetailLogService logService;

    /**
     * ReportCharacter ID로 상세 로그 조회
     * GET /report/character/detaillog/{report_character_id}
     * 
     * @param reportCharacterId ReportCharacter ID
     * @return 상세 로그 목록
     */
    @GetMapping("/detaillog/{report_character_id}")
    public ResponseEntity<Map<String, Object>> getLogs(@PathVariable("report_character_id") Integer reportCharacterId) {

        Map<String, Object> res = new HashMap<>();

        try {
            List<ReportCharacterDetailLogDto> logs = logService.getDetailLogsByReportCharacterId(reportCharacterId);

            res.put("code", 200);
            res.put("data", logs);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("code", 400);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }
}
