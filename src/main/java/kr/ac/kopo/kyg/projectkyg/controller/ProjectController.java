//package kr.ac.kopo.kyg.projectkyg.controller;
//
//import kr.ac.kopo.kyg.projectkyg.domain.Assignment;
//import kr.ac.kopo.kyg.projectkyg.domain.Team;
//import kr.ac.kopo.kyg.projectkyg.domain.User;
//import kr.ac.kopo.kyg.projectkyg.repository.AssignmentRepository;
//import kr.ac.kopo.kyg.projectkyg.repository.TeamRepository;
//import kr.ac.kopo.kyg.projectkyg.repository.UserRepository;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//import jakarta.transaction.Transactional;
//
//import java.security.Principal;
//import java.util.List;
//
//@Controller
//public class ProjectController {
//
//    private final TeamRepository teamRepository;
//    private final UserRepository userRepository;
//    private final AssignmentRepository assignmentRepository;
//
//    public ProjectController(TeamRepository teamRepository,
//                             UserRepository userRepository,
//                             AssignmentRepository assignmentRepository) {
//        this.teamRepository = teamRepository;
//        this.userRepository = userRepository;
//        this.assignmentRepository = assignmentRepository;
//    }
//
//    // 팀 과제 페이지
//    @GetMapping("/projects/{teamId}")
//    public String projectsPage(@PathVariable Long teamId, Model model, Principal principal) {
//        Team team = teamRepository.findById(teamId)
//                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));
//
//        model.addAttribute("team", team);
//
//        String username = principal != null ? principal.getName() : "Guest";
//        model.addAttribute("username", username);
//
//        // 팀 생성자인지 여부
//        boolean isCreator = principal != null && username.equals(team.getManagerUsername());
//        model.addAttribute("isCreator", isCreator);
//
//        // 팀 과제 목록
//        List<Assignment> assignments = assignmentRepository.findByTeamId(teamId);
//        model.addAttribute("assignments", assignments);
//
//        return "projects"; // templates/projects.html
//    }
//
//    // 과제 추가 페이지
//    @GetMapping("/projects/{teamId}/add")
//    public String addProjectPage(@PathVariable Long teamId, Model model, Principal principal) {
//        Team team = teamRepository.findById(teamId)
//                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));
//        model.addAttribute("team", team);
//        return "addproject"; // templates/add_assignment.html
//    }
//
//    // 과제 추가 처리
//    @PostMapping("/projects/{teamId}/add")
//    @Transactional
//    public String addProject(@PathVariable Long teamId,
//                             @RequestParam String name,
//                             @RequestParam String description,
//                             @RequestParam String deadline,
//                             Principal principal) {
//        Team team = teamRepository.findById(teamId)
//                .orElseThrow(() -> new IllegalStateException("팀을 찾을 수 없습니다."));
//
//        Assignment assignment = new Assignment();
//        assignment.setName(name);
//        assignment.setDescription(description);
//        assignment.setDeadline(java.time.LocalDateTime.parse(deadline));
//        assignment.setTeam(team);
//
//        assignmentRepository.save(assignment);
//
//        return "redirect:/projects/" + teamId;
//    }
//}
