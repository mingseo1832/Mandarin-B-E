package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.ReportCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportCharacterRepository extends JpaRepository<ReportCharacter, Integer> {

    /**
     * 캐릭터 ID로 ReportCharacter 목록 조회
     */
    List<ReportCharacter> findByCharacter_CharacterId(Long characterId);
}

