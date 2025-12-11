package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.ChatReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatReportRepository extends JpaRepository<ChatReport, Integer> {

    List<ChatReport> findByUser_Id(Long userId);

    List<ChatReport> findByCharacter_CharacterId(Long characterId);
}
