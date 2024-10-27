package com.bytedance.douyinclouddemo.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "player")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", length = 128, nullable = false, unique = true)
    private String userId;

    @Column(name = "user_name", length = 128, nullable = false)
    private String userName;

    @Column(name = "avatar_url", length = 512, nullable = false)
    private String avatarUrl;

    @Column(name = "score")
    private Long score = 0L;

    @Column(name = "glory")
    private Long glory = 0L;

    @Column(name = "ext", columnDefinition = "TEXT")
    private String ext = "{}";

    @Column(name = "game_count")
    private Integer gameCount = 0;

    @Column(name = "total_payment")
    private Integer totalPayment = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}