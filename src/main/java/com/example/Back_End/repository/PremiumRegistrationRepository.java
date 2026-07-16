package com.example.Back_End.repository;

import com.example.Back_End.entity.PremiumRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PremiumRegistrationRepository extends JpaRepository<PremiumRegistration, Long> {
    List<PremiumRegistration> findByUserEmailOrderByRegisteredAtDesc(String userEmail);
    Optional<PremiumRegistration> findTopByUserEmailAndPlanNameAndStatusOrderByRegisteredAtDesc(String userEmail, String planName, PremiumRegistration.Status status);
    List<PremiumRegistration> findByStatusOrderByRegisteredAtDesc(PremiumRegistration.Status status);
    List<PremiumRegistration> findByOrderByRegisteredAtDesc();
}
