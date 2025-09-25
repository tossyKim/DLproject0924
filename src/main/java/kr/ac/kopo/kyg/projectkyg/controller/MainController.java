package kr.ac.kopo.kyg.projectkyg.controller;

import kr.ac.kopo.kyg.projectkyg.domain.*;
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
import java.time.Duration;
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

        // 각 팀의 가장 가까운 마감시간 계산
        LocalDateTime now = LocalDateTime.now();
        for (Team team : userTeams) {
            List<Assignment> assignments = assignmentRepository.findByTeamId(team.getId());
            long minHours = assignments.stream()
                    .map(a -> Duration.between(now, a.getDeadline()).toHours())
                    .filter(hours -> hours >= 0) // 이미 지난 마감 제외
                    .min(Long::compare)
                    .orElse(-1L);
            team.setHoursUntilDeadline(minHours);
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

    /** 팀 수정 페이지 */
    @GetMapping("/teams/{id}/edit")
    public String editTeamForm(@PathVariable Long id, Authentication authentication, Model model) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 수정할 수 있습니다.");
        }

        model.addAttribute("team", team);
        return "team_edit";
    }

    /** 팀 수정 처리 */
    @Transactional
    @PostMapping("/teams/{id}/edit")
    public String updateTeam(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam String description,
                             @RequestParam(required = false) String password,
                             Authentication authentication) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 수정할 수 있습니다.");
        }

        team.setName(name);
        team.setDescription(description);
        if (password != null && !password.isBlank()) {
            team.setPassword(passwordEncoder.encode(password));
        }

        teamRepository.save(team);
        return "redirect:/projects/" + team.getId();
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
    /** 프로젝트 목록 페이지 */
    @GetMapping("/projects/{id}")
    public String projectsPage(@PathVariable Long id, Model model, Authentication authentication) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));
        model.addAttribute("team", team);

        List<Assignment> assignments = assignmentRepository.findByTeamId(id);

        String username = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElse("Guest");
        model.addAttribute("username", username);

        boolean isCreator = authentication != null &&
                authentication.getName().equals(team.getManagerUsername());
        model.addAttribute("isCreator", isCreator);

        // 현재 로그인 사용자가 제출한 과제인지 확인 후 Assignment 객체에 표시
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            for (Assignment assignment : assignments) {
                boolean submitted = submissionRepository
                        .findByAssignmentIdAndUserId(assignment.getId(), user.getId())
                        .isPresent();
                assignment.setSubmitted(submitted); // 새로운 필드로 제출 여부 표시
            }
        }

        // 현재 시간 기준으로 마감 정렬: 미마감 과제 먼저, 미마감 과제는 남은 시간 오름차순,
        // 마감된 과제는 그 뒤로
        LocalDateTime now = LocalDateTime.now();
        assignments.sort((a1, a2) -> {
            boolean a1Past = a1.getDeadline().isBefore(now);
            boolean a2Past = a2.getDeadline().isBefore(now);

            if (a1Past && !a2Past) return 1;    // a1 마감, a2 미마감 → a1 뒤로
            if (!a1Past && a2Past) return -1;   // a1 미마감, a2 마감 → a1 앞으로
            return a1.getDeadline().compareTo(a2.getDeadline()); // 둘 다 같은 상태이면 마감시간 순
        });

        model.addAttribute("assignments", assignments);
        model.addAttribute("now", now);

        return "projects";
    }


    /** 팀 해체 (팀장만 가능) */
    @Transactional
    @PostMapping("/teams/{id}/delete")
    public String deleteTeam(@PathVariable Long id, Authentication authentication) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 팀을 해체할 수 있습니다.");
        }

        for (User u : team.getUsers()) {
            u.getTeams().remove(team);
            userRepository.save(u);
        }

        teamRepository.delete(team);

        return "redirect:/main";
    }

    /** 팀 탈퇴 (팀원만 가능) */
    @Transactional
    @PostMapping("/teams/{id}/leave")
    public String leaveTeam(@PathVariable Long id, Authentication authentication) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));

        if (user.getUsername().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장은 탈퇴할 수 없습니다. 팀을 해체하세요.");
        }

        team.getUsers().remove(user);
        user.getTeams().remove(team);

        teamRepository.save(team);
        userRepository.save(user);

        return "redirect:/main";
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

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = assignment.getDeadline();
        String originalFileName = file.getOriginalFilename();
        String finalFileName = originalFileName;

        if (now.isAfter(deadline)) {
            if (existingSubmission.isPresent()) {
                finalFileName = "마감수정_" + originalFileName;
            } else {
                finalFileName = "마감제출_" + originalFileName;
            }
        }

        Submission submission;
        if (existingSubmission.isPresent()) {
            submission = existingSubmission.get();
            submission.setFileData(file.getBytes());
            submission.setFileName(finalFileName);
            submission.setSubmittedAt(LocalDateTime.now());
        } else {
            submission = new Submission();
            submission.setAssignment(assignment);
            submission.setUser(user);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setFileData(file.getBytes());
            submission.setFileName(finalFileName);
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

    /** 팀장용 제출물 확인 페이지 */
    @GetMapping("/projects/{teamId}/submissions")
    public String viewSubmissions(@PathVariable Long teamId,
                                  @RequestParam Long assignmentId,
                                  Authentication authentication,
                                  Model model) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("과제를 찾을 수 없습니다."));

        boolean isCreator = authentication != null &&
                authentication.getName().equals(team.getManagerUsername());

        if (!isCreator) {
            throw new IllegalStateException("팀장만 제출물을 볼 수 있습니다.");
        }

        List<Submission> submissions = submissionRepository.findByAssignmentId(assignmentId);

        model.addAttribute("team", team);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);

        return "project_submissions";
    }
    /** 팀원 관리 페이지 (팀장만 접근 가능) */
    @GetMapping("/teams/{teamId}/members")
    public String manageMembers(@PathVariable Long teamId, Model model, Authentication authentication) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 참가자를 관리할 수 있습니다.");
        }

        model.addAttribute("team", team);
        model.addAttribute("members", team.getUsers());

        return "manage_members";
    }
    /** 팀원 제거 (팀장만 가능) */
    @PostMapping("/teams/{teamId}/remove")
    @Transactional
    public String removeMember(@RequestParam Long memberId, @PathVariable Long teamId, Authentication authentication) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 팀원을 제거할 수 있습니다.");
        }

        if (user.getUsername().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장은 제거할 수 없습니다.");
        }

        team.getUsers().remove(user);
        user.getTeams().remove(team);

        teamRepository.save(team);
        userRepository.save(user);

        return "redirect:/teams/{teamId}/members";
    }

    /** 관리자 페이지 (ROLE_ADMIN 전용) */
    @GetMapping("/admin/users") // 기존 /main/admin -> /admin/users
    public String adminPage(Model model, Authentication authentication) {
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new IllegalStateException("관리자만 접근할 수 있습니다.");
        }

        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);
        return "admin_users"; // admin_users.html 반환
    }

    /** 유저 수정 처리 */
    @PostMapping("/admin/users/update") // 기존 /main/admin/users/update -> /admin/users/update
    @Transactional
    public String updateUser(@RequestParam Long userId,
                             @RequestParam String name,
                             @RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String role,
                             Authentication authentication) {
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new IllegalStateException("관리자만 접근할 수 있습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        user.setName(name);
        user.setUsername(username);
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setRole(Role.valueOf(role));

        userRepository.save(user);

        return "redirect:/admin/users"; // 관리자 페이지로 리다이렉트
    }

    /** 유저 삭제 처리 */
    @PostMapping("/admin/users/delete")
    @Transactional
    public String deleteUser(@RequestParam Long userId, Authentication authentication) {
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new IllegalStateException("관리자만 접근할 수 있습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
        userRepository.delete(user);

        return "redirect:/admin/users";
    }

}
