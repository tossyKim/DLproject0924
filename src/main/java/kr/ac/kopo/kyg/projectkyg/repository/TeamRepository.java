package kr.ac.kopo.kyg.projectkyg.repository;

import kr.ac.kopo.kyg.projectkyg.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    // 팀 이름으로 팀을 찾는 메소드
    Optional<Team> findByName(String name);

    // 팀 이름과 비밀번호로 팀을 찾는 메소드
    Optional<Team> findByNameAndPassword(String name, String password);
}