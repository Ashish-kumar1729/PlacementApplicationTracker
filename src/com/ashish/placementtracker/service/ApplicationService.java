package com.ashish.placementtracker.service;

import com.ashish.placementtracker.model.ApplicationStatus;
import com.ashish.placementtracker.model.PlacementApplication;
import com.ashish.placementtracker.repository.ApplicationRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ApplicationService {
    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public PlacementApplication add(String company, String role, LocalDate appliedOn,
            LocalDate deadline) throws IOException {
        if (appliedOn == null || appliedOn.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Application date must be today or earlier.");
        }
        PlacementApplication application = new PlacementApplication(repository.nextId(),
                company, role, appliedOn, deadline, ApplicationStatus.APPLIED);
        // Same company + role + date is considered a duplicate in this version.
        boolean duplicate = repository.findAll().stream().anyMatch(existing ->
                existing.getCompanyName().equalsIgnoreCase(application.getCompanyName())
                && existing.getRole().equalsIgnoreCase(application.getRole())
                && existing.getApplicationDate().equals(appliedOn));
        if (duplicate) throw new IllegalArgumentException("This company, role and application date already exist.");
        repository.save(application);
        return application;
    }

    public List<PlacementApplication> findAll() { return repository.findAll(); }

    public PlacementApplication findById(int id) {
        return repository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("No application exists with ID " + id + "."));
    }

    public List<PlacementApplication> search(String query) {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("Search cannot be empty.");
        String keyword = query.strip().toLowerCase(Locale.ROOT);
        return repository.findAll().stream().filter(application ->
                application.getCompanyName().toLowerCase(Locale.ROOT).contains(keyword)
                || application.getRole().toLowerCase(Locale.ROOT).contains(keyword)).toList();
    }

    public void updateStatus(int id, ApplicationStatus status) throws IOException {
        repository.save(findById(id).withStatus(status));
    }

    public void delete(int id) throws IOException {
        findById(id);
        repository.deleteById(id);
    }

    public List<PlacementApplication> sortByDeadline() {
        return repository.findAll().stream().sorted(Comparator
                .comparing(PlacementApplication::getDeadline)
                .thenComparingInt(PlacementApplication::getId)).toList();
    }

    public List<PlacementApplication> filterByStatus(ApplicationStatus status) {
        return repository.findAll().stream()
                .filter(application -> application.getStatus() == status).toList();
    }
}
