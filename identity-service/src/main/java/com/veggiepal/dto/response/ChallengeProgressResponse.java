package com.veggiepal.dto.response;

import java.time.LocalDate;

public record ChallengeProgressResponse(Long id, LocalDate startedOn, int completedDays,
                                        int totalDays, int points, String status, boolean checkedInToday) { }
