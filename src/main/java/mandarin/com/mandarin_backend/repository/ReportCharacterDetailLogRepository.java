package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.ReportCharacterDetailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportCharacterDetailLogRepository extends JpaRepository<ReportCharacterDetailLog, Long> {

    List<ReportCharacterDetailLog> findByReportCharacter_ChatReport_ChatReportIdOrderByTimestampAsc(Integer chatReportId);
}
