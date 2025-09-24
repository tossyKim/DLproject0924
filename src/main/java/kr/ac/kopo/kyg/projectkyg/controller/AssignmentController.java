//package kr.ac.kopo.kyg.projectkyg.controller;
//
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@Controller
//@RequestMapping("/assignments")
//public class AssignmentController {
//
//    public static final Map<Integer, List<Map<String, Object>>> teamAssignments = new HashMap<>();
//
//    // 제출된 과제들을 저장할 맵 (키: assignmentId, 값: 제출 목록)
//    public static final Map<Integer, List<Map<String, Object>>> assignmentSubmissions = new HashMap<>();
//
//    private final List<Map<String, Object>> teams;
//
//    public AssignmentController() {
//        this.teams = MainController.teams;
//    }
//
//    // 과제 게시 폼 페이지
//    @GetMapping("/create")
//    public String createAssignmentForm(@RequestParam int teamId, Model model) {
//        Map<String, Object> team = teams.stream()
//                .filter(t -> t.get("id").equals(teamId))
//                .findFirst()
//                .orElse(null);
//
//        if (team == null) {
//            return "redirect:/main";
//        }
//        model.addAttribute("team", team);
//        return "addproject";
//    }
//
//    // 과제 저장 처리
//    @PostMapping("/save")
//    public String saveAssignment(
//            @RequestParam int teamId,
//            @RequestParam String name,
//            @RequestParam String description,
//            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime deadline) {
//
//        Map<String, Object> newAssignment = new HashMap<>();
//
//        List<Map<String, Object>> assignments = teamAssignments.computeIfAbsent(teamId, k -> new ArrayList<>());
//
//        newAssignment.put("id", assignments.size() + 1);
//        newAssignment.put("name", name);
//        newAssignment.put("description", description);
//        newAssignment.put("deadline", deadline);
//
//        assignments.add(newAssignment);
//
//        return "redirect:/projects/" + teamId;
//    }
//
//    // 과제 제출 폼 페이지
//    @GetMapping("/submit")
//    public String submitAssignmentForm(@RequestParam int teamId, @RequestParam int assignmentId, Model model) {
//        Map<String, Object> assignment = teamAssignments.getOrDefault(teamId, new ArrayList<>()).stream()
//                .filter(a -> a.get("id").equals(assignmentId))
//                .findFirst()
//                .orElse(null);
//
//        if (assignment == null) {
//            return "redirect:/projects/" + teamId;
//        }
//
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        model.addAttribute("teamId", teamId);
//        model.addAttribute("assignment", assignment);
//        model.addAttribute("username", authentication.getName());
//        return "submitproject";
//    }
//
//    // 과제 제출 처리 (새로 추가)
//    @PostMapping("/submit")
//    public String submitAssignment(
//            @RequestParam int teamId,
//            @RequestParam int assignmentId,
//            @RequestParam("file") MultipartFile file) {
//
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String submitter = authentication.getName();
//
//        // 파일이 비어있는지 확인
//        if (file.isEmpty()) {
//            System.out.println("제출된 파일이 없습니다.");
//            return "redirect:/projects/" + teamId; // 또는 에러 메시지 표시
//        }
//
//        Map<String, Object> submission = new HashMap<>();
//        submission.put("assignmentId", assignmentId);
//        submission.put("submitter", submitter);
//        submission.put("fileName", file.getOriginalFilename());
//        submission.put("submittedAt", LocalDateTime.now());
//
//        List<Map<String, Object>> submissionsForAssignment = assignmentSubmissions.computeIfAbsent(assignmentId, k -> new ArrayList<>());
//        submissionsForAssignment.add(submission);
//
//        // 제출 확인을 위한 로그 출력
//        System.out.println("과제 제출 완료! 세부 정보:");
//        System.out.println("팀 ID: " + teamId);
//        System.out.println("과제 ID: " + assignmentId);
//        System.out.println("제출자: " + submitter);
//        System.out.println("파일명: " + file.getOriginalFilename());
//
//        return "redirect:/projects/" + teamId;
//    }
//}