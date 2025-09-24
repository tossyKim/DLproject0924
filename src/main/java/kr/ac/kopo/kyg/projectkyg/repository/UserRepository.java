package kr.ac.kopo.kyg.projectkyg.repository;

import kr.ac.kopo.kyg.projectkyg.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 기존 메서드: username으로 찾기 (회원가입 중복 확인에 사용)
    Optional<User> findByUsername(String username);

    // 새로운 메서드: username으로 찾으면서 관련된 팀 정보도 함께 가져옴
    @Query("SELECT u FROM User u JOIN FETCH u.teams WHERE u.username = :username")
    Optional<User> findByUsernameWithTeams(@Param("username") String username);
}