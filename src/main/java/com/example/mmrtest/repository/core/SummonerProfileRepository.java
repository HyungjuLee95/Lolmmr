package com.example.mmrtest.repository.core;

import com.example.mmrtest.entity.core.SummonerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummonerProfileRepository extends JpaRepository<SummonerProfile, String> {
    Optional<SummonerProfile> findByGameNameIgnoreCaseAndTagLineIgnoreCase(String gameName, String tagLine);
}
