package com.veggiepal.entity;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seven_day_challenges", indexes = @Index(name = "idx_challenge_user", columnList = "user_id,id"))
@Getter @Setter @NoArgsConstructor
public class SevenDayChallenge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;
    @Column(nullable = false, length = 20)
    private String status;

    public SevenDayChallenge(Long userId, LocalDate startedOn) {
        this.userId = userId;
        this.startedOn = startedOn;
        this.status = "IN_PROGRESS";
    }
}
