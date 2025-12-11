package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.ReportCharacterDetailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportCharacterDetailLogRepository extends JpaRepository<ReportCharacterDetailLog, Long> {

    /**
     * ReportCharacter ID로 상세 로그 조회 (시간순)
     */
    List<ReportCharacterDetailLog> findByReportCharacter_ReportCharacterIdOrderByTimestampAsc(Integer reportCharacterId);

    /**
     * ReportCharacter ID로 상세 로그 삭제
     */
    void deleteByReportCharacter_ReportCharacterId(Integer reportCharacterId);
}
