package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // characterId로 해당 캐릭터의 리포트 목록 조회
    List<Report> findByCharacter_CharacterId(Long characterId);

    // characterId로 가장 최근 리포트 1개 조회
    Report findTopByCharacter_CharacterIdOrderByCreatedAtDesc(Long characterId);
}

