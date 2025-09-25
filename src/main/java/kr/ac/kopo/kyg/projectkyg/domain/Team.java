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

    // 화면 표시용 필드: 가장 가까운 과제의 마감까지 남은 시간(시간 단위)
    @Transient
    private long hoursUntilDeadline;
}
