package kr.ac.kopo.kyg.projectkyg.repository;

import kr.ac.kopo.kyg.projectkyg.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    // 팀 ID로 과제 리스트 조회
    List<Assignment> findByTeamId(Long teamId);
}
