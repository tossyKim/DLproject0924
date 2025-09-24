package kr.ac.kopo.kyg.projectkyg.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 메인키

    @Column(nullable = false, length = 50)
    private String name; // 이름

    @Column(nullable = false, unique = true, length = 50)
    private String username; // 로그인 아이디 (학번)

    @Column(nullable = false, length = 100)
    private String password; // 비밀번호

    @Column(nullable = false)
    @Enumerated(EnumType.STRING) // 이 어노테이션이 핵심입니다.
    private Role role; // enum 타입으로 변경

    // 다대다 관계 설정
    @ManyToMany
    @JoinTable(
            name = "user_team", // 조인 테이블 이름
            joinColumns = @JoinColumn(name = "user_id"), // User 엔티티의 외래키
            inverseJoinColumns = @JoinColumn(name = "team_id") // Team 엔티티의 외래키
    )
    private Set<Team> teams = new HashSet<>();

}