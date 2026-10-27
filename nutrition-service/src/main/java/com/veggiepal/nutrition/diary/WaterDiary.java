package com.veggiepal.nutrition.diary;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;
import static com.veggiepal.nutrition.diary.DiaryDtos.*;
@Entity @Table(name="water_diary",indexes=@Index(name="idx_water_diary_user_date",columnList="user_id,entry_date"))
@Getter @Setter @NoArgsConstructor
public class WaterDiary {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="entry_date",nullable=false) private LocalDate date;
    @Column(nullable=false) private Integer milliliters;
    public WaterEntry view() { return new WaterEntry(id,date,milliliters); }
}
