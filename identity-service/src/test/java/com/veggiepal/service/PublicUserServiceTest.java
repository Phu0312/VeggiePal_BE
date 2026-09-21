package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.dto.response.PublicUserResponse;
import com.veggiepal.entity.User;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PublicUserServiceTest {

    @Mock
    UserRepository userRepository;

    @Spy
    UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    PublicUserService publicUserService;

    static User user(Long id, String fullName) {
        return User.builder()
                .id(id).email("a@b.com").fullName(fullName)
                .avatarUrl("http://minio/avatars/" + id + ".png")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void getPublicUsers_returnsOnlyNameAndAvatar() {
        when(userRepository.findByIdInAndStatus(List.of(1L), UserStatus.ACTIVE))
                .thenReturn(List.of(user(1L, "Long Nguyễn")));

        List<PublicUserResponse> result = publicUserService.getPublicUsers(List.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getFullName()).isEqualTo("Long Nguyễn");
        assertThat(result.getFirst().getAvatarUrl()).isNotNull();
    }

    // A missing or suspended account is simply absent; asking for it is not an error
    @Test
    void getPublicUsers_unknownId_isSkippedSilently() {
        when(userRepository.findByIdInAndStatus(List.of(1L, 99L), UserStatus.ACTIVE))
                .thenReturn(List.of(user(1L, "Long Nguyễn")));

        assertThat(publicUserService.getPublicUsers(List.of(1L, 99L))).hasSize(1);
    }

    @Test
    void getPublicUsers_emptyList_throwsInvalidRequest() {
        assertThatThrownBy(() -> publicUserService.getPublicUsers(List.of()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(userRepository, never()).findByIdInAndStatus(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    // Without a cap, one request could walk the whole users table
    @Test
    void getPublicUsers_moreThanFiftyIds_throwsInvalidRequest() {
        List<Long> ids = java.util.stream.LongStream.rangeClosed(1, 51).boxed().toList();

        assertThatThrownBy(() -> publicUserService.getPublicUsers(ids))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    // The no-leak guarantee is structural: this DTO carries exactly three fields.
    // A slice test cannot check it — a mocked service only populates what the test sets,
    // so a newly added email field stays null and the assertion passes regardless.
    @Test
    void publicUserResponse_exposesOnlyIdNameAndAvatar() {
        List<String> fields = Arrays.stream(PublicUserResponse.class.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(Field::getName)
                .toList();

        assertThat(fields).containsExactlyInAnyOrder("id", "fullName", "avatarUrl");
    }
}
