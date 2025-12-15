package mandarin.com.mandarin_backend.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.LoginRequest;
import mandarin.com.mandarin_backend.dto.LoginResponse;
import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request,
                               HttpSession session) {

        User user = authService.login(request);

        if (user == null) {
            return new LoginResponse(false, "아이디 또는 비밀번호가 올바르지 않습니다.", null, 0);
        }

        session.setAttribute("LOGIN_USER_ID", user.getId());
        session.setAttribute("LOGIN_USER", user.getUserId());

        return new LoginResponse(true, "로그인에 성공했습니다.", user.getId(), user.getLoveType());
    }

    @GetMapping("/me")
    public LoginResponse me(HttpSession session) {
        Long id = (Long) session.getAttribute("LOGIN_USER_ID");

        if (id == null) {
            return new LoginResponse(false, "로그인되어 있지 않습니다.", null, 0);
        }

        return new LoginResponse(true, "현재 로그인된 유저 ID = " + id, id, 0);
    }

    @PostMapping("/logout")
    public LoginResponse logout(HttpSession session) {
        session.invalidate();
        return new LoginResponse(true, "로그아웃 되었습니다.", null, 0);
    }
}
