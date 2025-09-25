package kr.ac.kopo.kyg.projectkyg.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "Submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime submittedAt;

    // 🔑 BLOB 방식 유지: 파일 데이터를 DB에 직접 저장
    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] fileData;

    // 사용자가 업로드한 원래 파일 이름
    private String fileName;

    // 💡 새로운 고유 이름 필드: MainController의 storedFilename을 저장하여 중복 이름 방지 및 관리 용이
    // 이 필드가 setStoredFileName 대신 사용되어야 합니다.
    private String storedUniqueName;


    // --- Getters and Setters (Lombok 사용 중이므로 수동 메서드는 제거하거나, 아래 수정을 따릅니다) ---
    // Lombok이 이미 Getter/Setter를 생성하지만, 누락된 필드만 명시적으로 추가하여 오류를 해결합니다.

    // 기존 Getter/Setter는 Lombok이 처리하거나 수동으로 이미 구현되어 있다고 가정합니다.

    // 💡 오류 해결: setStoredFileName 대신 사용할 Getter/Setter 추가
    public String getStoredUniqueName() {
        return storedUniqueName;
    }

    public void setStoredUniqueName(String storedUniqueName) {
        this.storedUniqueName = storedUniqueName;
    }
}