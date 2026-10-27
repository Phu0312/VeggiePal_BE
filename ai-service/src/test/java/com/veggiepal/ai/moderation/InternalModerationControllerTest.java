package com.veggiepal.ai.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import com.veggiepal.ai.moderation.ModerationDtos.ContentType;
import com.veggiepal.ai.moderation.ModerationDtos.ModerationRequest;
import com.veggiepal.ai.security.InternalServiceToken;
import org.junit.jupiter.api.Test;

class InternalModerationControllerTest {

    final ModerationService service = mock(ModerationService.class);
    final InternalModerationController controller =
            new InternalModerationController(service, new InternalServiceToken("secret-token"));
    final ModerationRequest request = new ModerationRequest(ContentType.BLOG, 42L, 7L, "text");

    @Test
    void wrongOrMissingToken_isRejectedWithoutModerating() {
        AppException wrong = assertThrows(AppException.class, () -> controller.check("other-token", request));
        assertEquals(ErrorCode.UNAUTHENTICATED, wrong.getErrorCode());
        assertThrows(AppException.class, () -> controller.check(null, request));

        verify(service, never()).moderateForService(any());
    }

    @Test
    void rightToken_moderatesOutsideTheUserBudget() {
        controller.check("secret-token", request);

        verify(service).moderateForService(request);
        verify(service, never()).moderate(any(), any());
    }
}
