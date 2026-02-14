package com.hiba.meeting_backend.Repository;

import com.hiba.meeting_backend.model.ReportSection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportSectionRepository extends MongoRepository<ReportSection,String> {


}
