package kr.ac.kopo.kyg.projectkyg.controller;

import kr.ac.kopo.kyg.projectkyg.domain.Role;
import kr.ac.kopo.kyg.projectkyg.domain.Team;
import kr.ac.kopo.kyg.projectkyg.domain.User;
import kr.ac.kopo.kyg.projectkyg.repository.TeamRepository;
import kr.ac.kopo.kyg.projectkyg.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.transaction.Transactional;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           TeamRepository teamRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 관리자 체크 유틸리티 */
    private void checkAdmin(Authentication authentication) {
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new IllegalStateException("관리자만 접근할 수 있습니다.");
        }
    }

    // --- 사용자 관리 ---

    /** 관리자 페이지 - 사용자 관리 */
    @GetMapping("/users")
    public String adminPage(Model model, Authentication authentication) {
        checkAdmin(authentication);

        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);
        return "admin_users";
    }

    /** 유저 수정 처리 */
    @PostMapping("/users/update")
    @Transactional
    public String updateUser(@RequestParam Long userId,
                             @RequestParam String name,
                             @RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String role,
                             Authentication authentication) {
        checkAdmin(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        user.setName(name);
        user.setUsername(username);
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setRole(Role.valueOf(role));

        userRepository.save(user);

        return "redirect:/admin/users";
    }

    /** 유저 삭제 처리 */
    @PostMapping("/users/delete")
    @Transactional
    public String deleteUser(@RequestParam Long userId, Authentication authentication) {
        checkAdmin(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
        userRepository.delete(user);

        return "redirect:/admin/users";
    }

    // --- 팀 관리 ---

    /** 관리자 페이지 - 팀 관리 */
    @GetMapping("/teams")
    public String adminTeams(Model model, Authentication authentication) {
        checkAdmin(authentication);

        List<Team> teams = teamRepository.findAll();
        model.addAttribute("teams", teams);
        return "admin_teams";
    }

    /** 팀 수정 처리 (관리자용) */
    @PostMapping("/teams/update")
    @Transactional
    public String updateTeamByAdmin(@RequestParam Long teamId,
                                    @RequestParam String name,
                                    @RequestParam String description,
                                    @RequestParam(required = false) String password,
                                    Authentication authentication) {
        checkAdmin(authentication);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        team.setName(name);
        team.setDescription(description);

        if (password != null && !password.isBlank()) {
            team.setPassword(passwordEncoder.encode(password));
        }

        teamRepository.save(team);
        return "redirect:/admin/teams";
    }


    /** 팀 삭제 처리 (관리자용) */
    @PostMapping("/teams/delete")
    @Transactional
    public String deleteTeamByAdmin(@RequestParam Long teamId, Authentication authentication) {
        checkAdmin(authentication);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        // 팀과 사용자 관계만 제거
        for (User user : team.getUsers()) {
            user.getTeams().remove(team);
            userRepository.save(user);
        }

        // Team 삭제 (Assignment와 Submission은 cascade로 자동 삭제)
        teamRepository.delete(team);

        return "redirect:/admin/teams";
    }
}