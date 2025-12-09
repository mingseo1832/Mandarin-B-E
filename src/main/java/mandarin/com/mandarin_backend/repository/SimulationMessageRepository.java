package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.Simulation;
import mandarin.com.mandarin_backend.entity.SimulationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationMessageRepository extends JpaRepository<SimulationMessage, Long> {

    /**
     * 특정 시뮬레이션의 모든 메시지를 시간순으로 조회
     */
    List<SimulationMessage> findBySimulationOrderByTimestampAsc(Simulation simulation);

    /**
     * 특정 시뮬레이션 ID의 모든 메시지를 시간순으로 조회
     */
    List<SimulationMessage> findBySimulationSimulationIdOrderByTimestampAsc(Long simulationId);

    /**
     * 특정 시뮬레이션의 메시지 수 조회
     */
    long countBySimulation(Simulation simulation);
}
