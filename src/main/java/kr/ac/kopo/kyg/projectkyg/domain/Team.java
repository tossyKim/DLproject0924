package kr.ac.kopo.kyg.projectkyg.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 200)
    private String description;

    // 팀장 정보 (문자열로 관리)
    @Column(nullable = false, length = 50)
    private String managerUsername;

    // 🟢 요청에 따라 유지된 DB 필드
    @Column(nullable = false, length = 50)
    private String managerName;

    @Column(nullable = false, length = 100)
    private String password;

    // 팀원과 다대다 관계 설정 (User 엔티티의 teams 필드에 의해 관리됨)
    @ManyToMany(mappedBy = "teams")
    private Set<User> users = new HashSet<>();

    // -----------------------------
    // Team과 Assignment의 1:N 관계
    @OneToMany(mappedBy = "team", cascade = CascadeType.REMOVE)
    private List<Assignment> assignments;
    // -----------------------------

    // 🟢 요청에 따라 유지된 필드 (이전 남은 시간 필드)
    @Transient
    private Long remainingMillis;

    // 🟢 미제출 과제 유무 (최신 요구 사항)
    @Transient
    private Boolean hasUnsubmittedAssignment;

    // 🟢 팀장 여부를 판단하기 위해 추가 (MainController에서 설정됨)
    @Transient
    private Boolean isTeamManager;

    // 🟢 MainController에서 User 객체를 설정하기 위한 임시 필드
    @Transient
    private User managerUser;

    // 💥 MainController의 team::setManagerUser 호출 오류를 해결합니다.
    public void setManagerUser(User managerUser) {
        this.managerUser = managerUser;
    }

    // 💥 Thymeleaf 오류 해결 핵심: MainController에서 설정할 isTeamManager의 Setter를 추가합니다.
    public void setIsTeamManager(Boolean isTeamManager) {
        this.isTeamManager = isTeamManager;
    }
}