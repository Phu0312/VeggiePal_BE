package com.veggiepal.entity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
@Entity @Table(name="user_follows",uniqueConstraints=@UniqueConstraint(name="uk_user_follow",
        columnNames={"follower_id","following_id"}),
        indexes=@Index(name="idx_user_follow_following",columnList="following_id,follower_id"))
@Getter @NoArgsConstructor
public class UserFollow {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="follower_id",nullable=false) private Long followerId;
    @Column(name="following_id",nullable=false) private Long followingId;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    public UserFollow(Long followerId,Long followingId) {
        this.followerId=followerId;this.followingId=followingId;this.createdAt=Instant.now();
    }
}
