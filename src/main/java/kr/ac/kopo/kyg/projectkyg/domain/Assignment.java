package kr.ac.kopo.kyg.projectkyg.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // 과제명

    @Column(length = 500)
    private String description; // 과제 설명

    @Column(nullable = false)
    private LocalDateTime deadline; // 마감일

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team; // 과제가 속한 팀
}
