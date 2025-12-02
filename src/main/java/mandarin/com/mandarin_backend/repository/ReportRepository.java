package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // 특정 유저 + 타입 기준으로 가장 최근 리포트 1개 조회
    Optional<Report> findTopByUserIdAndTypeOrderByCreatedAtDesc(String userId, String type);

    // 특정 유저 + 캐릭터 기준으로 가장 최근 채팅 리포트 1개
    Optional<Report> findTopByUserIdAndCharacterIdAndTypeOrderByCreatedAtDesc(
            String userId, Long characterId, String type
    );

    // 필요하면 리포트 전체 리스트 조회도 가능
    List<Report> findByUserIdAndType(String userId, String type);
}
