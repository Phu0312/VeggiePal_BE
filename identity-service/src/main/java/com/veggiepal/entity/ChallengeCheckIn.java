package com.veggiepal.entity;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "challenge_check_ins", uniqueConstraints =
        @UniqueConstraint(name = "uk_challenge_day", columnNames = {"challenge_id", "day_number"}))
@Getter @NoArgsConstructor
public class ChallengeCheckIn {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;
    @Column(name = "day_number", nullable = false)
    private int dayNumber;
    @Column(name = "checked_on", nullable = false)
    private LocalDate checkedOn;

    public ChallengeCheckIn(Long challengeId, int dayNumber, LocalDate checkedOn) {
        this.challengeId = challengeId;
        this.dayNumber = dayNumber;
        this.checkedOn = checkedOn;
    }
}
