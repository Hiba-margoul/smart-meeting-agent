package com.hiba.meeting_backend.Repository;

import com.hiba.meeting_backend.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByToken(String token);
}
