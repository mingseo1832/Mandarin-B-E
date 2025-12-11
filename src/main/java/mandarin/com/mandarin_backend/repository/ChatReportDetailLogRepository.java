package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.ChatReportDetailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatReportDetailLogRepository extends JpaRepository<ChatReportDetailLog, Long> {

    /**
     * 특정 ChatReport ID의 모든 상세 로그 조회 (시간순)
     */
    List<ChatReportDetailLog> findByChatReportIdOrderByTimestampAsc(Integer chatReportId);

    /**
     * 특정 ChatReport ID의 상세 로그 삭제
     */
    void deleteByChatReportId(Integer chatReportId);
}
