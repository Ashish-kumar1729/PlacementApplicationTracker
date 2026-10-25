# Complete project source

Each heading gives the file path relative to the project root.
See README.md for setup, walkthrough, limitations, and interview questions.

## `src/com/ashish/placementtracker/Main.java`

```java
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

```

## `src/com/ashish/placementtracker/model/ApplicationStatus.java`

```java
package com.ashish.placementtracker.model;

public enum ApplicationStatus {
    APPLIED, ASSESSMENT, INTERVIEW, OFFERED, REJECTED
}

```

## `src/com/ashish/placementtracker/model/PlacementApplication.java`

```java
package com.ashish.placementtracker.model;

import java.time.LocalDate;
import java.util.Objects;

/** Immutable: an update creates a new object instead of modifying stored state. */
public final class PlacementApplication {
    private final int id;
    private final String companyName;
    private final String role;
    private final LocalDate applicationDate;
    private final LocalDate deadline;
    private final ApplicationStatus status;

    public PlacementApplication(int id, String companyName, String role,
            LocalDate applicationDate, LocalDate deadline, ApplicationStatus status) {
        if (id <= 0) throw new IllegalArgumentException("ID must be positive.");
        this.id = id;
        this.companyName = requiredText(companyName, "Company");
        this.role = requiredText(role, "Role");
        this.applicationDate = Objects.requireNonNull(applicationDate, "Application date is required.");
        this.deadline = Objects.requireNonNull(deadline, "Deadline is required.");
        this.status = Objects.requireNonNull(status, "Status is required.");
        if (deadline.isBefore(applicationDate)) {
            throw new IllegalArgumentException("Deadline cannot be before application date.");
        }
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
        String text = value.strip();
        if (text.length() > 150) throw new IllegalArgumentException(field + " must be at most 150 characters.");
        if (text.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field + " cannot contain control characters.");
        }
        return text;
    }

    public int getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getRole() { return role; }
    public LocalDate getApplicationDate() { return applicationDate; }
    public LocalDate getDeadline() { return deadline; }
    public ApplicationStatus getStatus() { return status; }

    public PlacementApplication withStatus(ApplicationStatus newStatus) {
        return new PlacementApplication(id, companyName, role, applicationDate, deadline, newStatus);
    }
}

```

## `src/com/ashish/placementtracker/repository/ApplicationRepository.java`

```java
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

```

## `src/com/ashish/placementtracker/repository/FileApplicationRepository.java`

```java
package com.ashish.placementtracker.repository;

import com.ashish.placementtracker.model.ApplicationStatus;
import com.ashish.placementtracker.model.PlacementApplication;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/** Single-process file store. Never launch two instances against the same file. */
public final class FileApplicationRepository implements ApplicationRepository {
    private final Path file;
    private Map<Integer, PlacementApplication> applications = new LinkedHashMap<>();
    private int nextId = 1;

    public FileApplicationRepository(Path file) throws IOException {
        this.file = file.toAbsolutePath();
        load();
    }

    private void load() throws IOException {
        if (!Files.exists(file)) return;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        try {
            if (!"1".equals(required(properties, "formatVersion"))) {
                throw new IllegalArgumentException("Unsupported file format.");
            }
            int count = Integer.parseInt(required(properties, "count"));
            int loadedNextId = Integer.parseInt(required(properties, "nextId"));
            if (count < 0 || loadedNextId < 1) throw new IllegalArgumentException("Invalid file metadata.");
            Map<Integer, PlacementApplication> loaded = new LinkedHashMap<>();
            int highestId = 0;
            for (int i = 0; i < count; i++) {
                String prefix = "application." + i + ".";
                int id = Integer.parseInt(required(properties, prefix + "id"));
                PlacementApplication application = new PlacementApplication(id,
                        required(properties, prefix + "company"), required(properties, prefix + "role"),
                        LocalDate.parse(required(properties, prefix + "appliedOn")),
                        LocalDate.parse(required(properties, prefix + "deadline")),
                        ApplicationStatus.valueOf(required(properties, prefix + "status")));
                if (loaded.put(id, application) != null) throw new IllegalArgumentException("Duplicate ID.");
                highestId = Math.max(highestId, id);
            }
            if (loadedNextId <= highestId) throw new IllegalArgumentException("Invalid next ID.");
            applications = loaded;
            nextId = loadedNextId;
        } catch (RuntimeException ex) {
            throw new IOException("Saved data is invalid. Back up and inspect " + file
                    + ". It has not been overwritten.", ex);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Missing field: " + key);
        return value;
    }

    @Override public int nextId() { return nextId; }
    @Override public List<PlacementApplication> findAll() {
        return new ArrayList<>(applications.values());
    }
    @Override public Optional<PlacementApplication> findById(int id) {
        return Optional.ofNullable(applications.get(id));
    }

    @Override public void save(PlacementApplication application) throws IOException {
        Map<Integer, PlacementApplication> updated = new LinkedHashMap<>(applications);
        updated.put(application.getId(), application);
        int updatedNextId = Math.max(nextId, Math.addExact(application.getId(), 1));
        persist(updated, updatedNextId);
        // Change in-memory state only after the file was successfully saved.
        applications = updated;
        nextId = updatedNextId;
    }

    @Override public void deleteById(int id) throws IOException {
        if (!applications.containsKey(id)) throw new IllegalArgumentException("Application ID not found.");
        Map<Integer, PlacementApplication> updated = new LinkedHashMap<>(applications);
        updated.remove(id);
        persist(updated, nextId);
        applications = updated;
    }

    private void persist(Map<Integer, PlacementApplication> records, int updatedNextId) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("formatVersion", "1");
        properties.setProperty("count", Integer.toString(records.size()));
        properties.setProperty("nextId", Integer.toString(updatedNextId));
        int index = 0;
        for (PlacementApplication application : records.values()) {
            String prefix = "application." + index++ + ".";
            properties.setProperty(prefix + "id", Integer.toString(application.getId()));
            properties.setProperty(prefix + "company", application.getCompanyName());
            properties.setProperty(prefix + "role", application.getRole());
            properties.setProperty(prefix + "appliedOn", application.getApplicationDate().toString());
            properties.setProperty(prefix + "deadline", application.getDeadline().toString());
            properties.setProperty(prefix + "status", application.getStatus().name());
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "applications-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(writer, "Placement Application Tracker - do not edit while app is running");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}

```

## `src/com/ashish/placementtracker/service/ApplicationService.java`

```java
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

```

## `src/com/ashish/placementtracker/ui/ConsoleMenu.java`

```java
package com.ashish.placementtracker.ui;

import com.ashish.placementtracker.model.ApplicationStatus;
import com.ashish.placementtracker.model.PlacementApplication;
import com.ashish.placementtracker.service.ApplicationService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public final class ConsoleMenu {
    private final ApplicationService service;
    private final Scanner scanner;

    public ConsoleMenu(ApplicationService service, Scanner scanner) {
        this.service = service;
        this.scanner = scanner;
    }

    public void start() {
        while (true) {
            System.out.println("\nPLACEMENT APPLICATION TRACKER");
            System.out.println("1. Add application");
            System.out.println("2. View all applications");
            System.out.println("3. Search company or role");
            System.out.println("4. Find application by ID");
            System.out.println("5. Update application status");
            System.out.println("6. Delete application");
            System.out.println("7. Sort by deadline (earliest first)");
            System.out.println("8. Filter by status");
            System.out.println("9. Show status summary");
            System.out.println("0. Exit");
            try {
                int choice = readNumber("Choose an option: ");
                switch (choice) {
                    case 1 -> addApplication();
                    case 2 -> display(service.findAll());
                    case 3 -> display(service.search(readLine("Company or role keyword: ")));
                    case 4 -> display(List.of(service.findById(readNumber("Application ID: "))));
                    case 5 -> updateStatus();
                    case 6 -> deleteApplication();
                    case 7 -> display(service.sortByDeadline());
                    case 8 -> display(service.filterByStatus(readStatus()));
                    case 9 -> showSummary();
                    case 0 -> {
                        System.out.println("Goodbye! Changes were saved automatically.");
                        return;
                    }
                    default -> System.out.println("Choose a menu option from 0 to 9.");
                }
            } catch (EndOfInput ex) {
                System.out.println("\nInput closed. Exiting; completed changes are already saved.");
                return;
            } catch (IllegalArgumentException ex) {
                System.out.println("Input error: " + ex.getMessage());
            } catch (IOException ex) {
                System.out.println("Storage error: " + ex.getMessage());
                System.out.println("Change was not confirmed. Check file access and free disk space.");
            }
        }
    }

    private void addApplication() throws IOException {
        String company = readLine("Company name: ");
        String role = readLine("Role: ");
        LocalDate appliedOn = readDate("Application date (YYYY-MM-DD; Enter = today): ", true);
        LocalDate deadline = readDate("Application deadline (YYYY-MM-DD): ", false);
        PlacementApplication created = service.add(company, role, appliedOn, deadline);
        System.out.println("Application saved. ID: " + created.getId() + ", status: APPLIED");
    }

    private void updateStatus() throws IOException {
        int id = readNumber("Application ID: ");
        display(List.of(service.findById(id)));
        service.updateStatus(id, readStatus());
        System.out.println("Status updated and saved.");
    }

    private void deleteApplication() throws IOException {
        int id = readNumber("Application ID: ");
        display(List.of(service.findById(id)));
        if (readLine("Delete this application? Type yes to confirm: ").equalsIgnoreCase("yes")) {
            service.delete(id);
            System.out.println("Application deleted and saved.");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private void showSummary() {
        List<PlacementApplication> applications = service.findAll();
        System.out.println("Total applications: " + applications.size());
        for (ApplicationStatus status : ApplicationStatus.values()) {
            long count = applications.stream().filter(application -> application.getStatus() == status).count();
            System.out.println(status + ": " + count);
        }
    }

    private void display(List<PlacementApplication> applications) {
        if (applications.isEmpty()) {
            System.out.println("No matching applications found.");
            return;
        }
        for (PlacementApplication application : applications) {
            System.out.println("\nID: " + application.getId());
            System.out.println("Company: " + application.getCompanyName());
            System.out.println("Role: " + application.getRole());
            System.out.println("Applied on: " + application.getApplicationDate());
            System.out.println("Deadline: " + application.getDeadline());
            System.out.println("Status: " + application.getStatus());
        }
    }

    private ApplicationStatus readStatus() {
        ApplicationStatus[] statuses = ApplicationStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            System.out.println((i + 1) + ". " + statuses[i]);
        }
        while (true) {
            int choice = readNumber("Select status (1-" + statuses.length + "): ");
            if (choice >= 1 && choice <= statuses.length) return statuses[choice - 1];
            System.out.println("Please select a status from the list.");
        }
    }

    private LocalDate readDate(String prompt, boolean defaultToday) {
        while (true) {
            String input = readLine(prompt);
            if (defaultToday && input.isEmpty()) return LocalDate.now();
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException ex) {
                System.out.println("Enter a valid date such as 2026-11-05.");
            }
        }
    }

    private int readNumber(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException ex) {
                System.out.println("Enter a whole number, for example 1.");
            }
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) throw new EndOfInput();
        return scanner.nextLine().strip();
    }

    private static final class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}

```

## `test/com/ashish/placementtracker/TrackerTests.java`

```java
package com.ashish.placementtracker;

import com.ashish.placementtracker.model.ApplicationStatus;
import com.ashish.placementtracker.repository.FileApplicationRepository;
import com.ashish.placementtracker.service.ApplicationService;
import com.ashish.placementtracker.ui.ConsoleMenu;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Scanner;

/** Dependency-free regression checks: run with bash run.sh --test. */
public final class TrackerTests {
    private static int checks;
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
    private static void invalid(Runnable action, String name) {
        try { action.run(); } catch (IllegalArgumentException ex) { checks++; return; }
        throw new AssertionError(name);
    }
    private static void invalidAdd(ApplicationService service, String company, String role,
            LocalDate date, LocalDate deadline, String name) throws IOException {
        try { service.add(company, role, date, deadline); }
        catch (IllegalArgumentException ex) { checks++; return; }
        throw new AssertionError(name);
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("placement-tracker-tests-");
        try {
            Path file = directory.resolve("applications.properties");
            LocalDate today = LocalDate.now();
            ApplicationService service = new ApplicationService(new FileApplicationRepository(file));
            check(service.findAll().isEmpty(), "New store is empty");
            var first = service.add("  A&B = Labs : भारत  ", "Java Developer", today, today.plusDays(5));
            var second = service.add("Beta", "Backend Intern", today.minusDays(2), today.plusDays(1));
            check(first.getId() == 1 && second.getId() == 2, "Unique IDs");
            check(service.search("labs").size() == 1 && service.search("JAVA").size() == 1,
                    "Case-insensitive company and role search");
            check(service.sortByDeadline().get(0).getId() == second.getId(), "Chronological sorting");
            invalidAdd(service, "a&b = labs : भारत", "java developer", today, today.plusDays(1), "Duplicate rejected");
            invalidAdd(service, " ", "Role", today, today, "Blank company rejected");
            invalidAdd(service, "Gamma", " ", today, today, "Blank role rejected");
            invalidAdd(service, "Gamma", "Role", today, today.minusDays(1), "Invalid date order rejected");
            invalidAdd(service, "Gamma", "Role", today.plusDays(1), today.plusDays(2), "Future application rejected");
            invalid(() -> service.findById(999), "Missing ID rejected");
            invalid(() -> service.search(" "), "Empty search rejected");
            service.updateStatus(first.getId(), ApplicationStatus.INTERVIEW);
            check(first.getStatus() == ApplicationStatus.APPLIED, "Old object remains immutable");
            check(service.filterByStatus(ApplicationStatus.INTERVIEW).size() == 1, "Status filter");
            ApplicationService reloaded = new ApplicationService(new FileApplicationRepository(file));
            check(reloaded.findAll().size() == 2, "Records survive restart");
            check(reloaded.findById(1).getStatus() == ApplicationStatus.INTERVIEW, "Update persisted");
            check(reloaded.findById(1).getCompanyName().equals("A&B = Labs : भारत"), "Unicode and delimiters preserved");
            reloaded.delete(2);
            reloaded = new ApplicationService(new FileApplicationRepository(file));
            check(reloaded.findAll().size() == 1, "Deletion persisted");
            check(reloaded.add("Gamma", "SDE", today, today).getId() == 3, "Deleted IDs not reused after restart");

            // Console checks use isolated storage and scripted input, never the user's data.
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            PrintStream original = System.out;
            try (PrintStream output = new PrintStream(capture, true, StandardCharsets.UTF_8)) {
                System.setOut(output);
                String input = "abc\n1\nConsole Company\nSDE\nnot-a-date\n\n" + today
                        + "\n5\n1\n3\n3\nconsole\n7\n8\n3\n9\n6\n1\nno\n6\n1\nyes\n2\n0\n";
                ApplicationService consoleService = new ApplicationService(
                        new FileApplicationRepository(directory.resolve("console.properties")));
                new ConsoleMenu(consoleService, new Scanner(input)).start();
                check(consoleService.findAll().isEmpty(), "Console add, update, cancel delete, confirm delete");
                new ConsoleMenu(consoleService, new Scanner("")).start();
            } finally { System.setOut(original); }
            String consoleOutput = capture.toString(StandardCharsets.UTF_8);
            check(consoleOutput.contains("Enter a whole number") && consoleOutput.contains("Enter a valid date"),
                    "Invalid console input recovers");
            check(consoleOutput.contains("Input closed"), "End of input exits cleanly");

            Path blocked = directory.resolve("blocked");
            ApplicationService blockedService = new ApplicationService(
                    new FileApplicationRepository(blocked.resolve("data.properties")));
            Files.writeString(blocked, "A file prevents creating the directory");
            try {
                blockedService.add("Failure", "Role", today, today);
                throw new AssertionError("Expected storage error");
            } catch (IOException ex) {
                check(blockedService.findAll().isEmpty(), "Failed write does not commit in-memory state");
            }
            Files.delete(blocked);
            check(blockedService.add("Retry", "Role", today, today).getId() == 1,
                    "Failed write does not consume ID");

            Files.writeString(file, "formatVersion=1\ncount=1\nnextId=2\n");
            String corrupted = Files.readString(file);
            try {
                new FileApplicationRepository(file);
                throw new AssertionError("Invalid data should fail startup");
            } catch (IOException ex) {
                check(Files.readString(file).equals(corrupted), "Corrupt file is not silently overwritten");
            }
            System.out.println("PASS: " + checks + " checks. Tests used temporary storage only.");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}

```

## `run.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"

# Prefer an explicitly configured JDK, then Ashish's installed Android Studio JDK.
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
    JAVA_EXEC="$JAVA_HOME/bin/java"
    JAVAC_EXEC="$JAVA_HOME/bin/javac"
elif [[ -x "$HOME/Applications/android-studio/jbr/bin/javac" ]]; then
    JAVA_EXEC="$HOME/Applications/android-studio/jbr/bin/java"
    JAVAC_EXEC="$HOME/Applications/android-studio/jbr/bin/javac"
elif command -v javac >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
    JAVA_EXEC="$(command -v java)"
    JAVAC_EXEC="$(command -v javac)"
else
    echo "JDK 17 or newer required. Set JAVA_HOME to your JDK folder."
    exit 1
fi

mkdir -p out
find src -name '*.java' -print | sort > sources.txt
"$JAVAC_EXEC" --release 17 -encoding UTF-8 -d out @sources.txt

if [[ "${1:-}" == "--test" ]]; then
    find test -name '*.java' -print | sort > test-sources.txt
    "$JAVAC_EXEC" --release 17 -encoding UTF-8 -cp out -d out @test-sources.txt
    "$JAVA_EXEC" -cp out com.ashish.placementtracker.TrackerTests
else
    "$JAVA_EXEC" -cp out com.ashish.placementtracker.Main
fi

```
