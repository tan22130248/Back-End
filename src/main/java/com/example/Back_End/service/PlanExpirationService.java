package com.example.Back_End.service;

import com.example.Back_End.entity.User;
import com.example.Back_End.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PlanExpirationService {

    private final UserRepository userRepository;

    public PlanExpirationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void resetExpiredPlans() {
        List<User> expiredUsers = userRepository.findByPlanExpiresAtBeforeAndRole(LocalDate.now(), "USER");
        for (User user : expiredUsers) {
            user.setPlanType("FREE");
            user.setPlanExpiresAt(null);
        }
        if (!expiredUsers.isEmpty()) {
            userRepository.saveAll(expiredUsers);
        }
    }
}
