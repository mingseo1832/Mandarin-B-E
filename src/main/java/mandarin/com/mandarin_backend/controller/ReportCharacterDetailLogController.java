package mandarin.com.mandarin_backend.controller;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ReportCharacterDetailLogDto;
import mandarin.com.mandarin_backend.service.ReportCharacterDetailLogService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/report/chat")
public class ReportCharacterDetailLogController {

    private final ReportCharacterDetailLogService logService;

    @GetMapping("/detaillog/{chat_report_id}")
    public ResponseEntity<Map<String, Object>> getLogs(@PathVariable("chat_report_id") Integer chatReportId) {

        Map<String, Object> res = new HashMap<>();

        try {
            List<ReportCharacterDetailLogDto> logs = logService.getDetailLogsByChatReportId(chatReportId);

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
