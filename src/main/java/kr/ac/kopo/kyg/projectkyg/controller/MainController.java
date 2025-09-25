package kr.ac.kopo.kyg.projectkyg.controller;

import kr.ac.kopo.kyg.projectkyg.domain.Assignment;
import kr.ac.kopo.kyg.projectkyg.domain.Team;
import kr.ac.kopo.kyg.projectkyg.domain.User;
import kr.ac.kopo.kyg.projectkyg.domain.Submission;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.transaction.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        LocalDateTime now = LocalDateTime.now();
        for (Team team : userTeams) {
            List<Assignment> assignments = assignmentRepository.findByTeamId(team.getId());
            long minHours = assignments.stream()
                    .map(a -> Duration.between(now, a.getDeadline()).toHours())
                    .filter(hours -> hours >= 0)
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

    /** 팀 탈퇴 */
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

    // --- 과제 제출 기능 ---

    /** 과제 제출 폼 */
    @GetMapping("/assignments/{assignmentId}/submit")
    public String submitAssignmentForm(@PathVariable Long assignmentId,
                                       Model model,
                                       Authentication authentication) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("과제를 찾을 수 없습니다."));

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));

        // 기존 제출물 조회 및 모델에 추가 (HTML에서 제출 상태 확인용)
        Optional<Submission> existingSubmission = submissionRepository.findByAssignmentIdAndUserId(assignmentId, user.getId());

        model.addAttribute("assignment", assignment);
        model.addAttribute("user", user);
        model.addAttribute("submission", existingSubmission.orElse(null));
        model.addAttribute("username", user.getName());
        model.addAttribute("teamId", assignment.getTeam().getId());

        return "submit_project";
    }

    /** 과제 제출 처리 (POST) - 마감일 확인 및 파일명 수정 로직 포함 */
    @Transactional
    @PostMapping("/assignments/{assignmentId}/submit")
    public String saveSubmission(@PathVariable Long assignmentId,
                                 @RequestParam("file") MultipartFile file,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "제출할 파일을 선택해주세요.");
            return "redirect:/assignments/" + assignmentId + "/submit";
        }

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("과제를 찾을 수 없습니다."));

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("로그인된 사용자를 찾을 수 없습니다."));

        // 1. 마감일 체크 로직
        LocalDateTime now = LocalDateTime.now();
        boolean isLate = now.isAfter(assignment.getDeadline());

        // 2. 파일 데이터 및 메타데이터 준비 (BLOB 방식)
        String originalFilename = file.getOriginalFilename();

        // 지연 제출일 경우 파일명 앞에 접두사 추가
        if (isLate) {
            originalFilename = "[지연]" + originalFilename;
        }

        String storedUniqueName = UUID.randomUUID().toString() + "_" + originalFilename;

        byte[] fileData;
        try {
            // 파일을 byte 배열로 변환하여 메모리에 로드 (BLOB)
            fileData = file.getBytes();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "파일 처리 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/assignments/" + assignmentId + "/submit";
        }

        // 3. 기존 제출물 확인 및 업데이트 (재제출 처리)
        Optional<Submission> existingSubmission = submissionRepository.findByAssignmentIdAndUserId(assignmentId, user.getId());

        Submission submission;
        String message;
        if (existingSubmission.isPresent()) {
            submission = existingSubmission.get();
            message = "과제가 성공적으로 **수정 제출**되었습니다.";
        } else {
            submission = new Submission();
            submission.setUser(user);
            submission.setAssignment(assignment);
            message = "과제가 성공적으로 제출되었습니다.";
        }

        // 4. Submission 엔티티 업데이트/저장
        submission.setFileName(originalFilename); // 지연 제출 접두사가 붙은 파일명 저장
        submission.setStoredUniqueName(storedUniqueName);
        submission.setFileData(fileData);
        submission.setSubmittedAt(now); // 현재 시간을 제출 시간으로 저장

        submissionRepository.save(submission);

        // 5. 리다이렉트 전에 성공 메시지 전달
        redirectAttributes.addFlashAttribute("message", message);

        // 제출 후 현재 제출 페이지로 리다이렉트
        return "redirect:/assignments/" + assignmentId + "/submit";
    }

    /** 제출 파일 다운로드 */
    @GetMapping("/assignments/download/{submissionId}")
    public ResponseEntity<ByteArrayResource> downloadSubmission(@PathVariable Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalStateException("제출 파일을 찾을 수 없습니다."));

        // 파일 다운로드 시 파일명이 깨지지 않도록 인코딩 처리
        String encodedFileName = URLEncoder.encode(submission.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"")
                .body(new ByteArrayResource(submission.getFileData()));
    }

    // 🚨 이전 충돌을 일으킨 listSubmissions 메서드는 삭제되었습니다.
}