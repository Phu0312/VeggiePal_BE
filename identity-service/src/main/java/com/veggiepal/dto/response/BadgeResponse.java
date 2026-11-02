package com.veggiepal.dto.response;

import java.time.LocalDateTime;

public record BadgeResponse(Long id, String type, LocalDateTime grantedAt,
                            LocalDateTime revokedAt, String revocationReason) { }
