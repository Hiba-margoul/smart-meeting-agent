package com.hiba.meeting_backend.Repository;

import com.hiba.meeting_backend.model.Meet;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetRepository extends MongoRepository<Meet,String> {
    Meet findByLiveKitRoomName(String liveKitRoomName);
    List<Meet> findByHostId(String hostId);
    // Trouver les réunions actives
    List<Meet> findByStatus(Meet.MeetingStatus status);


    Meet findByTitle(String title);
}
