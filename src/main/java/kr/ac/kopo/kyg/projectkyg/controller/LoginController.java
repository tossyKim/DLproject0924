package kr.ac.kopo.kyg.projectkyg.controller;

import kr.ac.kopo.kyg.projectkyg.domain.User;
import kr.ac.kopo.kyg.projectkyg.domain.Role;
import kr.ac.kopo.kyg.projectkyg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public LoginController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/loginfailed")
    public String loginFailed(Model model) {
        model.addAttribute("error", "true");
        return "login";
    }

    @GetMapping("/logout")
    public String logout() {
        return "login";
    }

    @GetMapping("/signup")
    public String showSignupForm() {
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(@RequestParam String name, @RequestParam String username, @RequestParam String password, Model model) {
        // 1. 아이디 중복 확인
        if (userRepository.findByUsername(username).isPresent()) {
            model.addAttribute("error", "이미 존재하는 아이디입니다.");
            return "signup";
        }

        // 2. 새로운 User 객체 생성
        User newUser = new User();
        newUser.setName(name);
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password)); // 비밀번호 암호화
        newUser.setRole(Role.ROLE_USER); // 기본 권한 부여

        // 3. User 객체를 데이터베이스에 저장
        userRepository.save(newUser);

        System.out.println("새 사용자 등록: " + username);

        // 4. 회원가입 성공 후 로그인 페이지로 리디렉션
        return "redirect:/login";
    }
}