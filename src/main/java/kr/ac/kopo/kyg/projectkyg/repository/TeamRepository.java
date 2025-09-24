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
}