package kr.ac.kopo.kyg.projectkyg.controller;

import kr.ac.kopo.kyg.projectkyg.domain.Team;
import kr.ac.kopo.kyg.projectkyg.domain.User;
import kr.ac.kopo.kyg.projectkyg.repository.TeamRepository;
import kr.ac.kopo.kyg.projectkyg.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.Optional;

@Controller
public class MainController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    public MainController(UserRepository userRepository, TeamRepository teamRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/main")
    public String mainPage(Model model, Authentication authentication) {
        String usernameFromAuth = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElse("Guest");

        Optional<User> optionalUser = userRepository.findByUsername(usernameFromAuth);

        String displayName = "Guest";
        if (optionalUser.isPresent()) {
            User loggedInUser = optionalUser.get();
            // findByUsernameWithTeams 대신 일반 findByUsername 사용
            // teams 목록은 별도로 가져오거나, findByUsernameWithTeams를 사용해야 함
            // 여기서는 이름을 가져오는 로직만 수정
            model.addAttribute("teams", teamRepository.findAll()); // 임시로 findAll 사용
            displayName = loggedInUser.getName();
        } else {
            model.addAttribute("teams", Collections.emptyList());
        }

        model.addAttribute("username", displayName);

        boolean isManager = Optional.ofNullable(authentication)
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
                .orElse(false);
        model.addAttribute("isManager", isManager);

        return "main";
    }

    @GetMapping("/teams/create")
    public String createTeamForm(Model model, Authentication authentication) {
        User loggedInUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (loggedInUser != null) {
            model.addAttribute("username", loggedInUser.getName());
        } else {
            model.addAttribute("username", "Guest");
        }
        return "createTeam";
    }

    @PostMapping("/teams/save")
    public String saveTeam(@RequestParam String name,
                           @RequestParam String description,
                           @RequestParam String password, Authentication authentication) {
        Team newTeam = new Team();
        newTeam.setName(name);
        newTeam.setDescription(description);
        newTeam.setManagerUsername(authentication.getName());
        newTeam.setPassword(passwordEncoder.encode(password));

        teamRepository.save(newTeam);

        User loggedInUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));

        loggedInUser.getTeams().add(newTeam);
        userRepository.save(loggedInUser);

        System.out.println("새 팀 등록: " + name);
        return "redirect:/main";
    }
}