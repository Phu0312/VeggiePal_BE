package com.veggiepal.ai.video;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name="ai_video_jobs") @Getter @NoArgsConstructor
public class VideoJob {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false,length=30) private String status;
    @Column(name="original_filename",nullable=false,length=255) private String originalFilename;
    @Column(columnDefinition="LONGTEXT") private String transcript;
    @Column(name="extraction_json",columnDefinition="LONGTEXT") private String extractionJson;
    @Column(name="error_message",length=500) private String errorMessage;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    public VideoJob(Long userId,String filename){this.userId=userId;this.originalFilename=filename;this.status="PROCESSING";this.createdAt=LocalDateTime.now();this.updatedAt=createdAt;}
    public void complete(String transcript,String json){this.transcript=transcript;this.extractionJson=json;this.status="COMPLETED";this.updatedAt=LocalDateTime.now();}
    public void fail(String error){this.status="FAILED";this.errorMessage=error==null?null:error.substring(0,Math.min(500,error.length()));this.updatedAt=LocalDateTime.now();}
}
