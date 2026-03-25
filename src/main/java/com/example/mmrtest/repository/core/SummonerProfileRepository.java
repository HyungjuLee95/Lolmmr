package com.example.mmrtest.repository.core;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.mmrtest.entity.core.SummonerProfile;

public interface SummonerProfileRepository extends JpaRepository<SummonerProfile, String> {
    Optional<SummonerProfile> findByGameNameAndTagLine(String gameName, String tagLine);
}