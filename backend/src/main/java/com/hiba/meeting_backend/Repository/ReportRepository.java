package com.hiba.meeting_backend.Repository;

import com.hiba.meeting_backend.model.Report;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {
    Optional<Report> findByMeetId(String meetId);
}
