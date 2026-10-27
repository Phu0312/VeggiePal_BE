package com.veggiepal.nutrition.diary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class MealDiaryWriter {
    private final MealDiaryRepository repository;
    // Separate transaction: a unique-key race must roll back before the caller reads the winner.
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW) public MealDiary insert(MealDiary entry) { return repository.saveAndFlush(entry); }
}
