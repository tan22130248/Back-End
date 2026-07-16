package com.example.Back_End.repository;

import com.example.Back_End.entity.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    @Query(value = "SELECT * FROM advertisements ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Advertisement findRandom();

    @Query("SELECT COUNT(a) FROM Advertisement a")
    long countAll();
}
