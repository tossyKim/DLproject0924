package kr.ac.kopo.kyg.projectkyg.repository;

import kr.ac.kopo.kyg.projectkyg.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // username으로 User 찾기 (회원가입 중복 확인 등)
    Optional<User> findByUsername(String username);

    // username으로 User를 찾으면서 관련 팀 정보까지 함께 가져오기
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.teams WHERE u.username = :username")
    Optional<User> findByUsernameWithTeams(@Param("username") String username);
}
