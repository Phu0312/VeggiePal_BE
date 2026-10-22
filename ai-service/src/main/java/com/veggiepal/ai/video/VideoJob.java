package com.veggiepal.ai.video;

import java.time.LocalDateTime;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name="ai_video_jobs") @Getter @NoArgsConstructor
public class VideoJob {
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false,length=30) private String status;
    @Column(name="original_filename",nullable=false,length=255) private String originalFilename;
    @Column(columnDefinition="LONGTEXT") private String transcript;
    @Column(name="extraction_json",columnDefinition="LONGTEXT") private String extractionJson;
    @Column(name="error_message",length=500) private String errorMessage;
    @Column(name="temp_file_path",length=1000) private String tempFilePath;
    @Column(name="quota_reserved",nullable=false) private boolean quotaReserved;
    @Column(name="quota_refunded",nullable=false) private boolean quotaRefunded;
    @Column(name="quota_period_date") private LocalDate quotaPeriodDate;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    public VideoJob(Long userId,String filename){this(userId,filename,null,false);}
    public VideoJob(Long userId,String filename,String tempFilePath,boolean quotaReserved){this(userId,filename,tempFilePath,quotaReserved,null);}
    public VideoJob(Long userId,String filename,String tempFilePath,boolean quotaReserved,LocalDate quotaPeriodDate){this.userId=userId;this.originalFilename=filename;this.tempFilePath=tempFilePath;this.quotaReserved=quotaReserved;this.quotaPeriodDate=quotaPeriodDate;this.status=PROCESSING;this.createdAt=LocalDateTime.now();this.updatedAt=createdAt;}
    public boolean complete(String transcript,String json){if(!PROCESSING.equals(status))return false;this.transcript=transcript;this.extractionJson=json;this.status=COMPLETED;this.tempFilePath=null;this.updatedAt=LocalDateTime.now();return true;}
    public boolean fail(String error){if(!PROCESSING.equals(status))return false;this.status=FAILED;this.errorMessage=error==null?null:error.substring(0,Math.min(500,error.length()));this.tempFilePath=null;this.updatedAt=LocalDateTime.now();return true;}
    public void touch(){if(PROCESSING.equals(status))this.updatedAt=LocalDateTime.now();}
    public void markQuotaRefunded(){this.quotaRefunded=true;}
    public void purgeOutput(){if(!PROCESSING.equals(status)){this.transcript=null;this.extractionJson=null;}}
}
