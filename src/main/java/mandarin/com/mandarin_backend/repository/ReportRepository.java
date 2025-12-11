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

    /**
     * 사용자 ID로 모든 리포트 조회
     */
    List<ChatReport> findByUser_Id(Long userId);

    /**
     * 사용자별 전체 score_avg의 평균 계산
     */
    @Query("SELECT AVG(cr.scoreAvg) FROM ChatReport cr WHERE cr.user.id = :userId")
    Double calculateAvgScoreByUserId(@Param("userId") Long userId);

    /**
     * 사용자별 특정 label_key의 label_score 평균 계산
     */
    @Query("SELECT AVG(cr.labelScore) FROM ChatReport cr WHERE cr.user.id = :userId AND cr.labelKey = :labelKey")
    Double calculateAvgLabelScoreByUserIdAndLabelKey(@Param("userId") Long userId, @Param("labelKey") Integer labelKey);

    /**
     * 사용자의 최신 리포트 1개 조회
     */
    ChatReport findFirstByUser_IdOrderByCreatedAtDesc(Long userId);
}
