package com.veggiepal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.veggiepal.enums.Role;

import com.veggiepal.entity.User;
import com.veggiepal.enums.UserStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // task_be C12: every filter optional; keyword matches email or full name, any case
    @org.springframework.data.jpa.repository.Query("""
            select u from User u
            where (:status is null or u.status = :status)
              and (:role is null or u.role = :role)
              and (:keyword is null
                   or lower(u.email) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%')))
            """)
    Page<User> searchForAdmin(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("status") UserStatus status,
            @org.springframework.data.repository.query.Param("role") Role role,
            Pageable pageable
    );


    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<User> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.email = :email")
    Optional<User> lockByEmail(@org.springframework.data.repository.query.Param("email") String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByIdInAndStatus(Collection<Long> ids, UserStatus status);
}
