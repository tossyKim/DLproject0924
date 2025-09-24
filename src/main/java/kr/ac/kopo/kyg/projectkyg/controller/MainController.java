package kr.ac.kopo.kyg.projectkyg.controller;

import kr.ac.kopo.kyg.projectkyg.domain.Team;
import kr.ac.kopo.kyg.projectkyg.domain.User;
import kr.ac.kopo.kyg.projectkyg.domain.Assignment;
import kr.ac.kopo.kyg.projectkyg.repository.TeamRepository;
import kr.ac.kopo.kyg.projectkyg.repository.UserRepository;
import kr.ac.kopo.kyg.projectkyg.repository.AssignmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class MainController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public MainController(UserRepository userRepository, TeamRepository teamRepository,
                          AssignmentRepository assignmentRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/main")
    public String mainPage(Model model, Authentication authentication) {
        String usernameFromAuth = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElse("Guest");

        String displayName = "Guest";
        List<Team> userTeams = teamRepository.findTeamsByUsername(usernameFromAuth);

        Optional<User> optionalUser = userRepository.findByUsername(usernameFromAuth);
        if (optionalUser.isPresent()) {
            displayName = optionalUser.get().getName();
        }

        model.addAttribute("username", displayName);
        model.addAttribute("teams", userTeams);

        boolean isManager = Optional.ofNullable(authentication)
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
                .orElse(false);
        model.addAttribute("isManager", isManager);

        return "main";
    }

    @Transactional
    @PostMapping("/teams/save")
    public String saveTeam(@RequestParam String name,
                           @RequestParam String description,
                           @RequestParam String password,
                           Authentication authentication) {
        User loggedInUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));

        Team newTeam = new Team();
        newTeam.setName(name);
        newTeam.setDescription(description);
        newTeam.setManagerUsername(authentication.getName());
        newTeam.setManagerName(loggedInUser.getName());
        newTeam.setPassword(passwordEncoder.encode(password));

        newTeam.getUsers().add(loggedInUser);
        loggedInUser.getTeams().add(newTeam);

        teamRepository.save(newTeam);

        return "redirect:/main";
    }

    @GetMapping("/teams/create")
    public String createTeamForm(Model model, Authentication authentication) {
        String username = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElse("Guest");
        model.addAttribute("username", username);
        return "createTeam";
    }

    @GetMapping("/teams/join")
    public String joinTeamForm(Model model) {
        List<Team> allTeams = teamRepository.findAll();
        model.addAttribute("teams", allTeams);
        return "joinTeam";
    }

    @Transactional
    @PostMapping("/teams/join")
    public String joinTeam(@RequestParam Long id, @RequestParam String teamPassword,
                           Model model, Authentication authentication) {
        User loggedInUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));

        Optional<Team> optionalTeam = teamRepository.findById(id);

        if (optionalTeam.isEmpty() || !passwordEncoder.matches(teamPassword, optionalTeam.get().getPassword())) {
            model.addAttribute("error", "팀 정보 또는 비밀번호가 올바르지 않습니다.");
            model.addAttribute("teams", teamRepository.findAll());
            return "joinTeam";
        }

        Team teamToJoin = optionalTeam.get();

        if (teamToJoin.getUsers().contains(loggedInUser)) {
            model.addAttribute("error", "이미 해당 팀에 가입되어 있습니다.");
            model.addAttribute("teams", teamRepository.findAll());
            return "joinTeam";
        }

        teamToJoin.getUsers().add(loggedInUser);
        loggedInUser.getTeams().add(teamToJoin);

        teamRepository.save(teamToJoin);

        return "redirect:/main";
    }

    // 프로젝트(과제) 목록 페이지
    @GetMapping("/projects/{id}")
    public String projectsPage(@PathVariable Long id, Model model, Authentication authentication) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));
        model.addAttribute("team", team);

        String username = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElse("Guest");
        model.addAttribute("username", username);

        boolean isCreator = authentication != null && authentication.getName().equals(team.getManagerUsername());
        model.addAttribute("isCreator", isCreator);

        List<Assignment> assignments = assignmentRepository.findByTeamId(team.getId());
        model.addAttribute("assignments", assignments);

        return "projects";
    }

    // 과제 추가 페이지
    @GetMapping("/addproject")
    public String addProjectForm(@RequestParam Long teamId, Model model, Authentication authentication) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));
        model.addAttribute("team", team);

        String username = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElse("Guest");
        model.addAttribute("username", username);

        boolean isCreator = authentication != null && authentication.getName().equals(team.getManagerUsername());
        if (!isCreator) {
            throw new IllegalStateException("팀 생성자만 과제를 추가할 수 있습니다.");
        }
        return "addproject";
    }

    // 과제 저장
    @Transactional
    @PostMapping("/assignments/save")
    public String saveAssignment(@RequestParam Long teamId,
                                 @RequestParam String name,
                                 @RequestParam String description,
                                 @RequestParam String deadline,
                                 Authentication authentication) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        boolean isCreator = authentication != null && authentication.getName().equals(team.getManagerUsername());
        if (!isCreator) {
            throw new IllegalStateException("팀 생성자만 과제를 추가할 수 있습니다.");
        }

        Assignment assignment = new Assignment();
        assignment.setTeam(team);
        assignment.setName(name);
        assignment.setDescription(description);
        assignment.setDeadline(LocalDateTime.parse(deadline));

        assignmentRepository.save(assignment);

        return "redirect:/projects/" + teamId;
    }
}
