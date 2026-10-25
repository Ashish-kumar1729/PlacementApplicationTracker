package com.ashish.placementtracker;

import com.ashish.placementtracker.repository.ApplicationRepository;
import com.ashish.placementtracker.repository.FileApplicationRepository;
import com.ashish.placementtracker.service.ApplicationService;
import com.ashish.placementtracker.ui.ConsoleMenu;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

public final class Main {
    public static void main(String[] args) {
        Path dataFile = Path.of("data", "applications.properties");
        System.out.println("Data file: " + dataFile.toAbsolutePath());
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            ApplicationRepository repository = new FileApplicationRepository(dataFile);
            ApplicationService service = new ApplicationService(repository);
            ConsoleMenu menu = new ConsoleMenu(service, scanner);
            menu.start();
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("Cannot start: " + ex.getMessage());
            System.err.println("Back up the data file before changing it. No reset was performed.");
            System.exit(1);
        }
    }
}
