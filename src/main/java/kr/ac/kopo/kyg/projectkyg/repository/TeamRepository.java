package kr.ac.kopo.kyg.projectkyg.repository;

import kr.ac.kopo.kyg.projectkyg.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    // 팀 이름으로 팀을 찾는 메소드
    Optional<Team> findByName(String name);

    // 팀 이름과 비밀번호로 팀을 찾는 메소드
    Optional<Team> findByNameAndPassword(String name, String password);

    // 사용자의 username으로 팀 목록을 찾는 새로운 메서드
    @Query("SELECT t FROM Team t JOIN t.users u WHERE u.username = :username")
    List<Team> findTeamsByUsername(@Param("username") String username);

    // 🟢 추가: 특정 사용자가 팀장인 모든 팀을 찾는 메서드 (기존 유지)
    List<Team> findByManagerUsername(String managerUsername);

    // 🟢 새로 추가된 메서드: managerName이 문자열 "NULL"인 팀을 조회하여 고아 팀을 정리하는 데 사용
    /**
     * 특정 managerName(예: "NULL")을 가진 모든 팀을 조회합니다.
     * 이는 관리자가 탈퇴하여 managerName이 NULL 문자열로 설정된 '고아 팀'을 찾는 데 사용됩니다.
     */
    List<Team> findByManagerName(String managerName);
}