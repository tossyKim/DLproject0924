package kr.ac.kopo.kyg.projectkyg.controller;

import kr.ac.kopo.kyg.projectkyg.domain.Assignment;
import kr.ac.kopo.kyg.projectkyg.domain.Submission; // 👈 Submission import 추가
import kr.ac.kopo.kyg.projectkyg.domain.Team;
import kr.ac.kopo.kyg.projectkyg.domain.User;
import kr.ac.kopo.kyg.projectkyg.repository.AssignmentRepository;
import kr.ac.kopo.kyg.projectkyg.repository.SubmissionRepository;
import kr.ac.kopo.kyg.projectkyg.repository.TeamRepository;
import kr.ac.kopo.kyg.projectkyg.repository.UserRepository;
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
public class TeamManagerController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;

    public TeamManagerController(UserRepository userRepository,
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

    // --- 팀 생성/저장 ---

    /** 팀 생성 폼 */
    @GetMapping("teams/create")
    public String createTeamForm(Model model, Authentication authentication) {
        String username = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElse("Guest");
        model.addAttribute("username", username);
        return "createTeam";
    }

    /** 팀 저장 */
    @Transactional
    @PostMapping("teams/save")
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

    // --- 팀 정보 수정 ---

    /** 팀 수정 페이지 */
    @GetMapping("teams/{id}/edit")
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
    @PostMapping("teams/{id}/edit")
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

    // --- 팀 해체 ---

    /** 팀 해체 */
    @Transactional
    @PostMapping("teams/{id}/delete")
    public String deleteTeam(@PathVariable Long id, Authentication authentication) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 팀을 해체할 수 있습니다.");
        }

        // 팀과 사용자 관계만 제거
        for (User u : team.getUsers()) {
            u.getTeams().remove(team);
            userRepository.save(u);
        }

        // Team 삭제 (Assignment와 Submission은 cascade로 자동 삭제)
        teamRepository.delete(team);

        return "redirect:/main";
    }

    // --- 프로젝트 목록 ---

    /** 프로젝트 목록 페이지 */
    @GetMapping("/projects/{id}") // 원래 상대 경로였음 → 절대 경로로 변경해야 /projects/{id} 접근 가능
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

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            for (Assignment assignment : assignments) {
                boolean submitted = submissionRepository
                        .findByAssignmentIdAndUserId(assignment.getId(), user.getId())
                        .isPresent();
                assignment.setSubmitted(submitted);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        assignments.sort((a1, a2) -> {
            boolean a1Past = a1.getDeadline().isBefore(now);
            boolean a2Past = a2.getDeadline().isBefore(now);
            if (a1Past && !a2Past) return 1;
            if (!a1Past && a2Past) return -1;
            return a1.getDeadline().compareTo(a2.getDeadline());
        });

        model.addAttribute("assignments", assignments);
        model.addAttribute("now", now);

        return "projects";
    }

    // --- 팀 참가자 관리 ---

    /** 팀 참가자 관리 페이지 */
    @GetMapping("teams/{teamId}/members")
    public String manageTeamMembers(@PathVariable Long teamId, Model model, Authentication authentication) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        // 팀장만 접근 가능
        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 참가자를 관리할 수 있습니다.");
        }

        List<User> teamMembers = team.getUsers().stream().toList();
        model.addAttribute("team", team);
        model.addAttribute("members", teamMembers);

        return "manage_members"; // templates/manage_members.html
    }
    @GetMapping("/projects/{teamId}/add")
    public String addAssignmentForm(@PathVariable Long teamId, Model model, Authentication authentication) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        // 팀장만 접근 가능
        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 과제를 추가할 수 있습니다.");
        }

        Assignment assignment = new Assignment();
        assignment.setTeam(team); // 과제에 팀 설정

        model.addAttribute("team", team);
        model.addAttribute("assignment", assignment);
        return "add_assignment"; // templates/add_assignment.html
    }

    /** 과제 저장 처리 */
    @PostMapping("/projects/{teamId}/add")
    @Transactional
    public String saveAssignment(@PathVariable Long teamId,
                                 @RequestParam String name,
                                 @RequestParam String description,
                                 @RequestParam String deadline, // "yyyy-MM-dd'T'HH:mm" 형식
                                 Authentication authentication) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        // 팀장만 접근 가능
        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 과제를 추가할 수 있습니다.");
        }

        Assignment assignment = new Assignment();
        assignment.setTeam(team);
        assignment.setName(name); // 엔티티 필드명에 맞춤
        assignment.setDescription(description);
        assignment.setDeadline(LocalDateTime.parse(deadline)); // 문자열을 LocalDateTime으로 변환

        assignmentRepository.save(assignment);

        return "redirect:/projects/" + teamId; // 과제 등록 후 프로젝트 페이지로 이동
    }
    @PostMapping("/projects/{teamId}/assignments/{assignmentId}/delete")
    @Transactional
    public String deleteAssignment(@PathVariable Long teamId,
                                   @PathVariable Long assignmentId,
                                   Authentication authentication) {
        // 1. 팀 및 과제 존재 여부 확인
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("과제를 찾을 수 없습니다."));

        // 2. 권한 확인 (팀장만 삭제 가능)
        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 과제를 삭제할 수 있습니다.");
        }

        // 3. 과제가 해당 팀에 속하는지 확인 (선택 사항이지만 안정성을 위해)
        if (!assignment.getTeam().getId().equals(teamId)) {
            throw new IllegalStateException("과제 ID와 팀 ID가 일치하지 않습니다.");
        }

        // 4. 과제 삭제
        // Assignment 엔티티에 Submission에 대한 CascadeType.REMOVE 설정이 있으므로,
        // 과제 삭제 시 해당 과제의 모든 제출물이 자동으로 삭제됩니다.
        assignmentRepository.delete(assignment);

        return "redirect:/projects/" + teamId; // 과제 삭제 후 프로젝트 목록으로 리디렉션
    }

    // ---------------------------------------------------------------------------------

    /** 1. 특정 팀의 모든 과제 제출물 목록 조회 (팀 매니저 전용) */
    @GetMapping("/projects/{teamId}/all-submissions")
    public String listAllSubmissions(@PathVariable Long teamId, Model model, Authentication authentication) {

        // 1. 팀 존재 여부 및 권한 확인
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 팀의 모든 제출 목록을 확인할 수 있습니다.");
        }

        // 2. 해당 팀의 모든 과제 조회
        List<Assignment> assignments = assignmentRepository.findByTeamId(teamId);

        // 3. 모든 과제에 제출된 모든 제출물 조회 (SubmissionRepository의 findAllByAssignmentIn 사용)
        List<Submission> allSubmissions = submissionRepository.findAllByAssignmentIn(assignments);

        // 4. 모델에 데이터 추가
        model.addAttribute("team", team);
        model.addAttribute("assignments", assignments); // 과제 목록도 함께 전달
        model.addAttribute("submissions", allSubmissions); // 팀 전체 제출물 리스트
        model.addAttribute("isManager", true);

        // 5. 템플릿 반환
        return "all_team_submissions"; // templates/all_team_submissions.html
    }

    // ---------------------------------------------------------------------------------

    /** 2. 특정 과제의 제출물 목록 조회 (팀 매니저 전용 - 기존 충돌 메서드 수정) */
    // 기존의 /projects/{teamId}/submissions 경로를 /projects/{teamId}/assignments/{assignmentId}/submissions 로 변경
    @GetMapping("/projects/{teamId}/assignments/{assignmentId}/submissions")
    public String viewSubmissionsByAssignment(@PathVariable Long teamId,
                                              @PathVariable Long assignmentId, // @RequestParam에서 @PathVariable로 변경
                                              Model model,
                                              Authentication authentication) {

        // 1. 팀 및 과제 존재 여부 확인
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("과제를 찾을 수 없습니다."));

        // 2. 권한 확인 (팀장만 접근 가능)
        if (!authentication.getName().equals(team.getManagerUsername())) {
            throw new IllegalStateException("팀장만 제출물 목록을 확인할 수 있습니다.");
        }

        // 3. 과제가 해당 팀에 속하는지 확인
        if (!assignment.getTeam().getId().equals(teamId)) {
            throw new IllegalStateException("과제 ID와 팀 ID가 일치하지 않습니다.");
        }

        // 4. 제출물 목록 조회 (SubmissionRepository의 findByAssignmentId 사용)
        List<Submission> submissions = submissionRepository.findByAssignmentId(assignmentId);

        // 5. 모델에 데이터 추가
        model.addAttribute("team", team);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions); // 제출물 리스트 추가

        // 6. 템플릿 반환
        return "project_submissions"; // templates/project_submissions.html
    }
}