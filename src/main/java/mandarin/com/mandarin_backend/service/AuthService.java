package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.LoginRequest;
import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public User login(LoginRequest request) {
        return userRepository.findByUserId(request.getUserId())
                .filter(user -> user.getPassword().equals(request.getPassword()))
                .orElse(null);
    }
}
