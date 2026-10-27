package com.veggiepal.repository;
import com.veggiepal.entity.*;
import com.veggiepal.enums.UserStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow,Long> {
    boolean existsByFollowerIdAndFollowingId(Long followerId,Long followingId);
    long deleteByFollowerIdAndFollowingId(Long followerId,Long followingId);
    @Query("select u from User u where u.status=:status and u.id in (select f.followingId from UserFollow f where f.followerId=:id)")
    Page<User> following(@Param("id") Long id,@Param("status") UserStatus status,Pageable pageable);
    @Query("select u from User u where u.status=:status and u.id in (select f.followerId from UserFollow f where f.followingId=:id)")
    Page<User> followers(@Param("id") Long id,@Param("status") UserStatus status,Pageable pageable);
    @Query("select count(u) from User u where u.status=:status and u.id in (select f.followingId from UserFollow f where f.followerId=:id)")
    long followingCount(@Param("id") Long id,@Param("status") UserStatus status);
    @Query("select count(u) from User u where u.status=:status and u.id in (select f.followerId from UserFollow f where f.followingId=:id)")
    long followerCount(@Param("id") Long id,@Param("status") UserStatus status);
    @Query("select u.id from User u where u.status=:status and u.id in (select f.followingId from UserFollow f where f.followerId=:id) order by u.id")
    List<Long> followingIds(@Param("id") Long id,@Param("status") UserStatus status);
}
