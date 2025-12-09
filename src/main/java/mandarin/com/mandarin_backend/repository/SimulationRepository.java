package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {
    
    // character_id로 시뮬레이션 목록 조회
    List<Simulation> findByCharacterCharacterId(Long characterId);
}

