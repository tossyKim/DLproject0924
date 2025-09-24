package kr.ac.kopo.kyg.projectkyg.controller;

import kr.ac.kopo.kyg.projectkyg.domain.Assignment;
import kr.ac.kopo.kyg.projectkyg.domain.Submission;
import kr.ac.kopo.kyg.projectkyg.domain.Team;
import kr.ac.kopo.kyg.projectkyg.domain.User;
import kr.ac.kopo.kyg.projectkyg.repository.AssignmentRepository;
import kr.ac.kopo.kyg.projectkyg.repository.SubmissionRepository;
import kr.ac.kopo.kyg.projectkyg.repository.TeamRepository;
import kr.ac.kopo.kyg.projectkyg.repository.UserRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class MainController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;

    public MainController(UserRepository userRepository,
                          TeamRepository teamRepository,
                          AssignmentRepository assignmentRepository,
                          SubmissionRepository submissionRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 메인 페이지 */
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

    /** 팀 생성 폼 */
    @GetMapping("/teams/create")
    public String createTeamForm(Model model, Authentication authentication) {
        String username = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElse("Guest");
        model.addAttribute("username", username);
        return "createTeam";
    }

    /** 팀 저장 */
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

    /** 팀 가입 폼 */
    @GetMapping("/teams/join")
    public String joinTeamForm(Model model) {
        List<Team> allTeams = teamRepository.findAll();
        model.addAttribute("teams", allTeams);
        return "joinTeam";
    }

    /** 팀 가입 처리 */
    @Transactional
    @PostMapping("/teams/join")
    public String joinTeam(@RequestParam Long id,
                           @RequestParam String teamPassword,
                           Model model,
                           Authentication authentication) {
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

    /** 프로젝트 목록 페이지 */
    @GetMapping("/projects/{id}")
    public String projectsPage(@PathVariable Long id, Model model, Authentication authentication) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));
        model.addAttribute("team", team);

        List<Assignment> assignments = assignmentRepository.findByTeamId(id);
        model.addAttribute("assignments", assignments);

        String username = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElse("Guest");
        model.addAttribute("username", username);

        boolean isCreator = authentication != null && authentication.getName().equals(team.getManagerUsername());
        model.addAttribute("isCreator", isCreator);

        return "projects";
    }

    /** 과제 추가 페이지 폼 */
    @GetMapping("/projects/{teamId}/add")
    public String addAssignmentForm(@PathVariable Long teamId, Model model) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));
        model.addAttribute("team", team);
        return "add_assignment";
    }

    /** 과제 저장 처리 */
    @Transactional
    @PostMapping("/projects/{teamId}/add")
    public String saveAssignment(@PathVariable Long teamId,
                                 @RequestParam String name,
                                 @RequestParam String description,
                                 @RequestParam String deadline,
                                 Authentication authentication) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        LocalDateTime deadlineDateTime = LocalDateTime.parse(deadline);

        Assignment assignment = new Assignment();
        assignment.setName(name);
        assignment.setDescription(description);
        assignment.setDeadline(deadlineDateTime);
        assignment.setTeam(team);

        assignmentRepository.save(assignment);

        return "redirect:/projects/" + teamId;
    }

    /** 과제 제출 페이지 */
    @GetMapping("/assignments/submit")
    public String submitAssignmentForm(@RequestParam Long assignmentId,
                                       @RequestParam Long teamId,
                                       Authentication authentication,
                                       Model model) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("과제를 찾을 수 없습니다."));

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));

        Optional<Submission> existingSubmission = submissionRepository.findByAssignmentIdAndUserId(assignmentId, user.getId());

        model.addAttribute("assignment", assignment);
        model.addAttribute("teamId", teamId);
        model.addAttribute("submission", existingSubmission.orElse(null));

        return "submit_project";
    }

    /** 과제 제출 및 수정 처리 */
    @PostMapping("/assignments/submit")
    @Transactional
    public String submitAssignment(@RequestParam Long assignmentId,
                                   @RequestParam Long teamId,
                                   @RequestParam("file") MultipartFile file,
                                   Authentication authentication,
                                   Model model) throws IOException {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("과제를 찾을 수 없습니다."));

        Optional<Submission> existingSubmission = submissionRepository.findByAssignmentIdAndUserId(assignmentId, user.getId());

        Submission submission;
        if (existingSubmission.isPresent()) {
            submission = existingSubmission.get();
            submission.setFileData(file.getBytes());
            submission.setFileName(file.getOriginalFilename());
            submission.setSubmittedAt(LocalDateTime.now());
        } else {
            submission = new Submission();
            submission.setAssignment(assignment);
            submission.setUser(user);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setFileData(file.getBytes());
            submission.setFileName(file.getOriginalFilename());
        }

        submissionRepository.save(submission);

        return "redirect:/assignments/submit?assignmentId=" + assignmentId + "&teamId=" + teamId;
    }

    /** 제출 파일 다운로드 */
    @GetMapping("/assignments/download/{submissionId}")
    public ResponseEntity<ByteArrayResource> downloadSubmission(@PathVariable Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("제출 파일을 찾을 수 없습니다."));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + submission.getFileName() + "\"")
                .body(new ByteArrayResource(submission.getFileData()));
    }
}
