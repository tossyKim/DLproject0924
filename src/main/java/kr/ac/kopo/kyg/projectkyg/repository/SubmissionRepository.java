package kr.ac.kopo.kyg.projectkyg.repository;

import jakarta.transaction.Transactional;
import kr.ac.kopo.kyg.projectkyg.domain.Assignment; // 👈 Assignment import 추가
import kr.ac.kopo.kyg.projectkyg.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    // ---------------------------------------------------------------------

    /** 💡 추가: 주어진 과제 목록에 해당하는 모든 제출물 조회 */
    // TeamManagerController에서 팀 전체 제출물을 조회하는 데 사용됩니다.
    List<Submission> findAllByAssignmentIn(List<Assignment> assignments);

    // ---------------------------------------------------------------------

    /** 제출물 이름으로 검색 (사용자가 지정한 파일 이름 기준) */
    List<Submission> findByFileNameContaining(String fileName);

    /** 특정 과제에 대해 제출일시 순으로 정렬하여 조회 */
    List<Submission> findByAssignmentIdOrderBySubmittedAtAsc(Long assignmentId);

    /** 저장된 실제 파일 이름으로 제출물 조회 */
    Optional<Submission> findByStoredUniqueName(String storedUniqueName);

    /** 사용자 이름으로 검색 */
    @Query("SELECT s FROM Submission s JOIN s.user u WHERE u.username LIKE %:username%")
    List<Submission> findByUserUsernameContaining(String username);

    @Transactional
    void deleteAllByAssignmentId(Long assignmentId);
}