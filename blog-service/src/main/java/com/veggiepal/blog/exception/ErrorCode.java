package com.veggiepal.blog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Shared codes: keep the same numbers as identity-service and nutrition-service
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_KEY(1001, "Invalid validation key", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED(1008, "Unauthenticated", HttpStatus.UNAUTHORIZED),

    UNAUTHORIZED(1009, "You do not have permission", HttpStatus.FORBIDDEN),

    FILE_UPLOAD_FAILED(1017, "Could not upload file, please try again later", HttpStatus.SERVICE_UNAVAILABLE),

    INVALID_REQUEST(1018, "Invalid request data", HttpStatus.BAD_REQUEST),

    // Category
    CATEGORY_NAME_REQUIRED(3001, "Category name is required", HttpStatus.BAD_REQUEST),

    CATEGORY_TYPE_REQUIRED(3002, "Category type is required", HttpStatus.BAD_REQUEST),

    CATEGORY_NOT_EXISTED(3003, "Category not existed", HttpStatus.NOT_FOUND),

    CATEGORY_NAME_DUPLICATED(3004, "A category with this name already exists under the same parent", HttpStatus.BAD_REQUEST),

    CATEGORY_IN_USE(3005, "Category is still used by blogs or child categories", HttpStatus.BAD_REQUEST),

    CATEGORY_DEPTH_EXCEEDED(3006, "Category tree is limited to two levels", HttpStatus.BAD_REQUEST),

    CATEGORY_INACTIVE(3007, "Category is not active", HttpStatus.BAD_REQUEST),

    CATEGORY_ID_REQUIRED(3008, "Category is required", HttpStatus.BAD_REQUEST),

    // Blog
    BLOG_TITLE_REQUIRED(3010, "Blog title is required", HttpStatus.BAD_REQUEST),

    INVALID_BLOG_TITLE(3011, "Blog title must be at most {max} characters", HttpStatus.BAD_REQUEST),

    BLOG_CONTENT_REQUIRED(3012, "Blog content is required", HttpStatus.BAD_REQUEST),

    INVALID_BLOG_CONTENT(3013, "Blog content must be at least {min} characters", HttpStatus.BAD_REQUEST),

    BLOG_NOT_EXISTED(3014, "Blog not existed", HttpStatus.NOT_FOUND),

    INVALID_BLOG_STATUS_TRANSITION(3015, "Blog is not in a state that allows this action", HttpStatus.BAD_REQUEST),

    // Thumbnail
    THUMBNAIL_REQUIRED(3020, "Thumbnail file is required", HttpStatus.BAD_REQUEST),

    INVALID_THUMBNAIL_TYPE(3021, "Thumbnail must be a JPEG, PNG or WEBP image", HttpStatus.BAD_REQUEST),

    THUMBNAIL_TOO_LARGE(3022, "Thumbnail must not exceed 5MB", HttpStatus.BAD_REQUEST),

    // Comment
    COMMENT_CONTENT_REQUIRED(3030, "Comment content is required", HttpStatus.BAD_REQUEST),

    INVALID_COMMENT_CONTENT(3031, "Comment must be at most {max} words", HttpStatus.BAD_REQUEST),

    COMMENT_NOT_EXISTED(3032, "Comment not existed", HttpStatus.NOT_FOUND),

    COMMENT_REPLY_TOO_DEEP(3033, "Replies are limited to one level", HttpStatus.BAD_REQUEST),

    INVALID_COMMENT_PARENT(3034, "Parent comment belongs to different content", HttpStatus.BAD_REQUEST),

    COMMENT_TARGET_NOT_EXISTED(3035, "The content being commented on does not exist", HttpStatus.BAD_REQUEST),

    UNSUPPORTED_TARGET_TYPE(3036, "This content type is not supported yet", HttpStatus.BAD_REQUEST),

    COMMENT_TOO_LONG(3037, "Comment must be at most {max} characters", HttpStatus.BAD_REQUEST),

    // Vote
    INVALID_VOTE_VALUE(3040, "Vote value must be 1 or -1", HttpStatus.BAD_REQUEST),

    CANNOT_VOTE_OWN_CONTENT(3041, "You cannot vote on your own content", HttpStatus.BAD_REQUEST),

    BLOG_IDS_REQUIRED(3042, "Blog id list is required", HttpStatus.BAD_REQUEST);

    ErrorCode(
            int code,
            String message,
            HttpStatusCode statusCode
    ) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    final int code;

    final String message;

    final HttpStatusCode statusCode;
}
