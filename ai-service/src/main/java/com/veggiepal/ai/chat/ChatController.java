package com.veggiepal.ai.chat;

import static com.veggiepal.ai.chat.ChatDtos.*;
import java.util.List;
import com.veggiepal.ai.common.ApiResponse;
import com.veggiepal.ai.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService service;
    private final GuestSessionService guestSessions;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/guest-sessions")
    ApiResponse<GuestSessionResponse> createGuestSession(HttpServletRequest request) {
        GuestSession session = guestSessions.create(clientIpResolver.resolve(request));
        return ApiResponse.success(new GuestSessionResponse(session.getId(), session.getExpiresAt()));
    }

    @PostMapping("/guest")
    ApiResponse<ChatReply> guest(@RequestBody @Valid GuestChatRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(service.guest(request.guestSession(), request.message(),
                clientIpResolver.resolve(servletRequest)));
    }
    @PostMapping ApiResponse<ChatReply> chat(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid ChatRequest request) {
        return ApiResponse.success(service.member(CurrentUser.id(jwt), request));
    }
    @GetMapping("/conversations") ApiResponse<List<ConversationSummary>> list(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.list(CurrentUser.id(jwt)));
    }
    @GetMapping("/conversations/{id}") ApiResponse<ConversationDetail> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ApiResponse.success(service.detail(CurrentUser.id(jwt), id));
    }
    @DeleteMapping("/conversations/{id}") ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
        return ApiResponse.success(null);
    }
}
