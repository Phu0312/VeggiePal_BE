package com.veggiepal.repository;

import java.util.Optional;
import com.veggiepal.entity.EmailOtp;
import com.veggiepal.enums.OtpPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findFirstByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from EmailOtp o where o.email = :email and o.purpose = :purpose")
    Optional<EmailOtp> findCurrentForUpdate(@Param("email") String email, @Param("purpose") OtpPurpose purpose);

    // Scalar identity hint only: never preload a managed OTP before waiting for the user lock.
    @Query("select o.email from EmailOtp o where o.resetTokenHash = :hash")
    Optional<String> findEmailByResetTokenHash(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from EmailOtp o where o.resetTokenHash = :hash")
    Optional<EmailOtp> findResetTokenForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("delete from EmailOtp o where o.email = :email and o.purpose = :purpose")
    void deleteByEmailAndPurpose(@Param("email") String email, @Param("purpose") OtpPurpose purpose);
}
