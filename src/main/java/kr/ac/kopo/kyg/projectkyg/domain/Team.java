package kr.ac.kopo.kyg.projectkyg.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, length = 50)
    private String managerUsername;

    @Column(nullable = false, length = 100)
    private String password;

    // 다대다 관계 설정 (User 엔티티의 teams 필드에 의해 관리됨)
    @ManyToMany(mappedBy = "teams")
    private Set<User> users = new HashSet<>();
}