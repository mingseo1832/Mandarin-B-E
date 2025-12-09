package mandarin.com.mandarin_backend.repository;

import mandarin.com.mandarin_backend.entity.UserCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {

    // User_Character 엔티티에 User 필드 이름이 "user" 라고 가정
    // @ManyToOne private User user;
    List<UserCharacter> findByUser_Id(Long userId);
}
