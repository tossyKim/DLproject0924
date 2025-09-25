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

        // 1. 🟢 이름 변경 여부를 미리 확인
        String oldName = user.getName();
        boolean nameChanged = !oldName.equals(name);

        // 사용자 엔티티 정보 업데이트
        user.setName(name);
        user.setUsername(username);
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setRole(Role.valueOf(role));

        userRepository.save(user);

        // 2. 🟢 이름이 변경되었다면 팀장 이름 동기화 로직 수행
        if (nameChanged) {
            // 해당 사용자가 팀장인 모든 팀을 찾습니다.
            List<Team> teamsToUpdate = teamRepository.findByManagerUsername(user.getUsername());

            // 모든 팀의 managerName을 새 이름으로 업데이트합니다.
            for (Team team : teamsToUpdate) {
                team.setManagerName(name);
            }
            // 변경된 팀 엔티티들을 일괄 저장합니다.
            teamRepository.saveAll(teamsToUpdate);
        }

        return "redirect:/admin/users";
    }

    /** 유저 삭제 처리 */
    @PostMapping("/users/delete")
    @Transactional
    public String deleteUser(@RequestParam Long userId, Authentication authentication) {
        checkAdmin(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        // 1. 🟢 해당 유저가 팀장인 모든 팀의 manager 정보를 'NULL' 문자열로 초기화
        List<Team> managedTeams = teamRepository.findByManagerUsername(user.getUsername());

        for (Team team : managedTeams) {
            // NOT NULL 제약 조건 유지를 위해 문자열 "NULL" 사용
            team.setManagerName("NULL");
            team.setManagerUsername("NULL");
        }
        // 변경된 팀 엔티티들을 일괄 저장합니다.
        teamRepository.saveAll(managedTeams);

        // 2. 유저가 멤버로 속한 팀에서 해당 유저를 제거합니다. (ManyToMany 관계 해제)
        for (Team team : user.getTeams()) {
            team.getUsers().remove(user);
            userRepository.save(user);
        }

        // 3. 유저 삭제
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

    /** 🟢 팀 일괄 정리 처리 (managerName이 "NULL"인 고아 팀 삭제) */
    @PostMapping("/teams/cleanup")
    @Transactional
    public String cleanupOrphanTeams(Authentication authentication) {
        checkAdmin(authentication);

        // 1. managerName이 "NULL" 문자열인 팀을 모두 조회
        // (TeamRepository에 findByManagerName("NULL") 메서드가 정의되어 있어야 합니다.)
        List<Team> orphanTeams = teamRepository.findByManagerName("NULL");

        // 2. 각 팀을 순회하며 삭제 처리
        for (Team team : orphanTeams) {
            // 팀 삭제 전에, 팀과 사용자 관계를 먼저 제거 (다대다 관계 해제)
            for (User user : team.getUsers()) {
                user.getTeams().remove(team);
                userRepository.save(user);
            }

            // 팀 삭제
            teamRepository.delete(team);
        }

        return "redirect:/admin/teams";
    }
}