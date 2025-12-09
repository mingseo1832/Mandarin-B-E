package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.Simulation;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    // 캐릭터의 모든 시뮬레이션 조회
    List<Simulation> findByCharacter(UserCharacter character);

    // 캐릭터 ID로 시뮬레이션 조회
    List<Simulation> findByCharacterCharacterId(Long characterId);

    // 캐릭터의 가장 최근 시뮬레이션 조회
    Optional<Simulation> findTopByCharacterOrderByTimeDesc(UserCharacter character);

    // 완료되지 않은 시뮬레이션 조회
    List<Simulation> findByCharacterAndIsFinishedFalse(UserCharacter character);
}

