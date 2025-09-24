package kr.ac.kopo.kyg.projectkyg.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
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
}
