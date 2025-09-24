package kr.ac.kopo.kyg.projectkyg.repository;

import kr.ac.kopo.kyg.projectkyg.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /** 특정 과제에 대한 모든 제출 조회 */
    List<Submission> findByAssignmentId(Long assignmentId);

    /** 특정 과제에 대한 특정 사용자의 제출 조회 */
    Optional<Submission> findByAssignmentIdAndUserId(Long assignmentId, Long userId);

    /** 특정 사용자가 제출한 모든 제출물 조회 */
    List<Submission> findByUserId(Long userId);

    /** 특정 팀 과제의 모든 제출 조회 (팀ID는 Assignment 객체를 통해 접근) */
    List<Submission> findByAssignment_TeamId(Long teamId);

    /** 제출물 이름으로 검색 */
    List<Submission> findByFileNameContaining(String fileName);
}
