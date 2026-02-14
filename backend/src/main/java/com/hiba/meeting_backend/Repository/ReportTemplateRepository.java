package com.hiba.meeting_backend.Repository;

import com.hiba.meeting_backend.model.ReportTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface ReportTemplateRepository extends MongoRepository<ReportTemplate, String> {
    Optional<ReportTemplate> findByName(String name);
    List<ReportTemplate> findByCreatedBy(String managerId);


}
