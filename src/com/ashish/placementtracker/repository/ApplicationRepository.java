package com.ashish.placementtracker.repository;

import com.ashish.placementtracker.model.PlacementApplication;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/** Storage contract. A JDBC implementation can replace file storage later. */
public interface ApplicationRepository {
    int nextId();
    List<PlacementApplication> findAll();
    Optional<PlacementApplication> findById(int id);
    void save(PlacementApplication application) throws IOException;
    void deleteById(int id) throws IOException;
}
