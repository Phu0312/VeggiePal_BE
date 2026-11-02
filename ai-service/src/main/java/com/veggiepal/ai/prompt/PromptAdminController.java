package com.veggiepal.ai.prompt;

import com.veggiepal.ai.common.ApiResponse;
import com.veggiepal.ai.security.CurrentUser;
import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/admin/prompts")
public class PromptAdminController {
    private final PromptManagementService management;
    private final PromptPreviewService preview;

    public PromptAdminController(PromptManagementService management, PromptPreviewService preview) {
        this.management = management; this.preview = preview;
    }

    @GetMapping
    ApiResponse<List<PromptManagementService.PromptSummary>> list() {
        return ApiResponse.success(management.list());
    }

    @GetMapping("/{key}")
    ApiResponse<PromptManagementService.PromptSummary> one(@PathVariable PromptKey key) {
        return ApiResponse.success(management.summary(key));
    }

    @GetMapping("/{key}/versions")
    ApiResponse<List<PromptManagementService.PromptVersionView>> versions(@PathVariable PromptKey key) {
        return ApiResponse.success(management.history(key));
    }

    @GetMapping("/{key}/activations")
    ApiResponse<List<PromptActivation>> activations(@PathVariable PromptKey key) {
        return ApiResponse.success(management.activationHistory(key));
    }

    @PostMapping("/{key}/drafts")
    ApiResponse<PromptManagementService.PromptVersionView> create(@AuthenticationPrincipal Jwt jwt,
            @PathVariable PromptKey key, @RequestBody(required = false) ContentRequest request) {
        return ApiResponse.success(management.createDraft(key, CurrentUser.id(jwt),
                request == null ? null : request.content()));
    }

    @PutMapping("/{key}/drafts/{version}")
    ApiResponse<PromptManagementService.PromptVersionView> edit(@PathVariable PromptKey key,
            @PathVariable int version, @RequestBody ContentRequest request) {
        if (request == null) throw new AppException(ErrorCode.INVALID_REQUEST);
        return ApiResponse.success(management.editDraft(key, version, request.content()));
    }

    @PostMapping("/{key}/drafts/{version}/preview")
    ApiResponse<PromptPreviewService.PreviewResult> preview(@PathVariable PromptKey key,
            @PathVariable int version, @RequestBody PreviewRequest request) {
        if (request == null) throw new AppException(ErrorCode.INVALID_REQUEST);
        return ApiResponse.success(preview.preview(key, version, request.sampleInput(), request.variables()));
    }

    @PostMapping("/{key}/drafts/{version}/publish")
    ApiResponse<PromptManagementService.PromptVersionView> publish(@AuthenticationPrincipal Jwt jwt,
            @PathVariable PromptKey key, @PathVariable int version) {
        return ApiResponse.success(management.publish(key, version, CurrentUser.id(jwt)));
    }

    @PostMapping("/{key}/versions/{version}/activate")
    ApiResponse<PromptManagementService.PromptSummary> activate(@AuthenticationPrincipal Jwt jwt,
            @PathVariable PromptKey key, @PathVariable int version) {
        return ApiResponse.success(management.restore(key, version, CurrentUser.id(jwt)));
    }

    public record ContentRequest(String content) { }
    public record PreviewRequest(String sampleInput, Map<String, String> variables) { }
}
