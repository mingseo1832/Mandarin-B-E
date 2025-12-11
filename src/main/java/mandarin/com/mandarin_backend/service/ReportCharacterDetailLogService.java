package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ReportCharacterDetailLogDto;
import mandarin.com.mandarin_backend.entity.ReportCharacterDetailLog;
import mandarin.com.mandarin_backend.repository.ReportCharacterDetailLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportCharacterDetailLogService {

    private final ReportCharacterDetailLogRepository logRepository;

    /**
     * ReportCharacter ID로 상세 로그 조회
     * 
     * @param reportCharacterId ReportCharacter ID
     * @return 상세 로그 DTO 목록
     */
    public List<ReportCharacterDetailLogDto> getDetailLogsByReportCharacterId(Integer reportCharacterId) {
        List<ReportCharacterDetailLog> logs =
                logRepository.findByReportCharacter_ReportCharacterIdOrderByTimestampAsc(reportCharacterId);

        if (logs.isEmpty()) {
            throw new IllegalArgumentException("대화 로그가 없습니다.");
        }

        return logs.stream()
                .map(ReportCharacterDetailLogDto::fromEntity)
                .collect(Collectors.toList());
    }
}
