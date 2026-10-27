package com.veggiepal.nutrition.diary;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;
import static com.veggiepal.nutrition.diary.DiaryDtos.*;
@Entity @Table(name="activity_diary",indexes=@Index(name="idx_activity_diary_user_date",columnList="user_id,entry_date"))
@Getter @Setter @NoArgsConstructor
public class ActivityDiary {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="entry_date",nullable=false) private LocalDate date;
    @Column(name="activity_type",nullable=false,length=100) private String activityType;
    @Column(nullable=false) private Integer minutes;
    public ActivityEntry view() { return new ActivityEntry(id,date,activityType,minutes); }
}
