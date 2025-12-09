package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.ChatReport;
import mandarin.com.mandarin_backend.entity.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<ChatReport, Integer> {

    /**
     * Simulation 엔티티로 조회
     */
    Optional<ChatReport> findBySimulation(Simulation simulation);

    /**
     * Simulation ID로 조회
     */
    @Query("SELECT cr FROM ChatReport cr WHERE cr.simulation.simulationId = :simulationId")
    Optional<ChatReport> findBySimulationId(@Param("simulationId") Long simulationId);

    /**
     * 캐릭터 ID로 모든 리포트 조회 (최신순)
     */
    @Query("SELECT cr FROM ChatReport cr " +
           "WHERE cr.simulation.character.characterId = :characterId " +
           "ORDER BY cr.createdAt DESC")
    List<ChatReport> findReportsByCharacterId(@Param("characterId") Long characterId);

    /**
     * 캐릭터 ID로 최신 리포트 1개 조회
     */
    ChatReport findFirstBySimulation_Character_CharacterIdOrderByCreatedAtDesc(Long characterId);
}
