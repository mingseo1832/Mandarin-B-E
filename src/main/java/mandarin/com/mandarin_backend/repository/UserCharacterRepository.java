package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {

    // 사용자의 모든 캐릭터 조회
    List<UserCharacter> findByUser(User user);

    // 사용자 ID로 모든 캐릭터 조회
    List<UserCharacter> findByUserId(Long userId);

    // 단일 조건 조회
    Optional<UserCharacter> findByCharacterName(String characterName);

    // 복합 조건 조회
    Optional<UserCharacter> findByUserAndCharacterName(User user, String characterName);

    Optional<UserCharacter> findByUserAndKakaoName(User user, String kakaoName);
}
