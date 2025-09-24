package kr.ac.kopo.kyg.projectkyg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.security.Principal;

@Controller
public class ProjectController {

    // 과제 추가 페이지로 이동 (팀 생성자만 접근)
    @GetMapping("/projects/{teamId}/add")
    public String addProjectPage(@PathVariable Long teamId, Model model, Principal principal) {
        // 팀 ID를 모델에 전달
        model.addAttribute("teamId", teamId);
        return "addproject"; // addproject.html
    }

    // 과제 추가 처리 (POST)
    @PostMapping("/projects/{teamId}/add")
    public String addProject(@PathVariable Long teamId,
                             @RequestParam String projectName,
                             @RequestParam String projectDescription,
                             Principal principal) {
        // 실제 프로젝트 생성 로직 작성
        // 예: 프로젝트를 DB에 저장
        // projectService.createProject(teamId, projectName, projectDescription, principal.getName());

        // 과제 목록 페이지로 리다이렉트
        return "redirect:/projects/" + teamId;
    }

    // 필요한 경우 과제 수정, 삭제 등 추가 매핑 작성 가능
}
