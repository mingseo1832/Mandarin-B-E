package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.ChatReportAvg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatReportAvgRepository extends JpaRepository<ChatReportAvg, Long> {

    /**
     * 유저 ID로 ChatReportAvg 목록 조회
     */
    List<ChatReportAvg> findByUser_Id(Long userId);
}

