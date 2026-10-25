# Placement Application Tracker

A single-user, terminal-based core Java project for learning OOP, collections,
validation, exceptions, dates, interfaces, and persistent file storage.
This version is not an Android or Spring Boot application.

## Requirements

- JDK 17 or newer (both `java` and `javac`).
- Bash for the supplied launcher. No Maven, Gradle, database, or external library.
- A terminal; VS Code or IntelliJ IDEA can be used to edit the source.

## Ubuntu quick start

1. Extract the ZIP. Keep the `PlacementApplicationTracker` folder together.
2. Open a terminal in that folder (right-click an empty area in Files -> Open in Terminal).
3. Run:

```bash
bash run.sh
```

The launcher automatically tries Android Studio's JDK at
`$HOME/Applications/android-studio/jbr` if JAVA_HOME is not a valid JDK.
To explicitly select it:

```bash
JAVA_HOME="$HOME/Applications/android-studio/jbr" bash run.sh
```

Do not paste Java code into the terminal. Edit `.java` files in an editor, save,
and run the launcher again. Do not put this project inside your Android project.

## Manual compile/run (alternative)

Run from this project's root directory:

```bash
mkdir -p out
find src -name '*.java' -print > sources.txt
"$HOME/Applications/android-studio/jbr/bin/javac" --release 17 -encoding UTF-8 -d out @sources.txt
"$HOME/Applications/android-studio/jbr/bin/java" -cp out com.ashish.placementtracker.Main
```

`javac` compiles source into `.class` files. `-d out` selects their output folder.
`--release 17` keeps the code compatible with Java 17 even when compiling with
Java 25. `-cp out` tells the JVM where compiled classes live.
The fully qualified main class includes its package name.

## Features and rules

- Add a company, role, application date, and application deadline; initial status APPLIED.
- View all, find by ID, search company/role ignoring case, filter by status.
- Update status, confirm deletion, sort by deadline, show status counts.
- Save after every successful mutation and reload on startup.
- Company and role must contain 1-150 characters after trimming; no control characters.
- Application date must be today or earlier. Deadline must be on/after application date.
- Historical applications are allowed: deadlines do not have to be in the future.
- Same company + role (ignoring case) + application date is a duplicate in this version.
- Status corrections are unrestricted; there is no enforced recruitment state machine.
- Invalid input prints a message; invalid field combinations cancel the current add
  operation and return to the main menu. Invalid number/date syntax is prompted again.

## Data and limitations

The app creates `data/applications.properties` after your first change.
We use Java's built-in `Properties` format instead of the earlier proposed `.dat`
binary file: it avoids Java object serialization and requires no parsing library.
Properties escapes delimiters; the reader/writer uses UTF-8. Copy the data file
while the program is closed to back it up. Do not commit personal records to Git.

Records are held in a LinkedHashMap keyed by integer ID; insertion order is stable.
IDs keep increasing even after deleting the highest ID and restarting.
The app writes a temporary file then replaces the saved file (atomic replacement
where supported), and commits its in-memory state only after persistence succeeds.
This reduces partial-write risk; it is not a database transaction or a guarantee
against all hardware/power failures. No automatic backup is created.

Run only ONE instance against the data file. Multi-process locking and multi-user
access are out of scope. Data is local plaintext, not encrypted. Corrupt records
cause startup to fail instead of silently resetting the file. Back up and inspect
the data if startup fails; never delete it casually to fix an error.

The app tracks applications you already submitted. It does not submit applications,
send alerts, authenticate users, or provide a web interface.

## Source structure and reading order

| File under src/com/ashish/placementtracker | What to learn |
| --- | --- |
| model/ApplicationStatus.java | Enum: a fixed set of allowed values |
| model/PlacementApplication.java | Encapsulation, constructor validation, immutable objects |
| repository/ApplicationRepository.java | Interface: a contract independent of storage technology |
| repository/FileApplicationRepository.java | Map, Optional, file I/O, try-with-resources, persistence |
| service/ApplicationService.java | Business rules, search, Comparator, stream operations |
| ui/ConsoleMenu.java | Menu loop, switch, Scanner, input parsing, exception handling |
| Main.java | Entry point and constructor-based dependency injection |

`test/.../TrackerTests.java` contains regression checks; `run.sh` compiles/runs;
`out/` is generated, not source. `data/` holds runtime data. `ALL_CODE.md`
collects the complete source in one reading document.

The call flow for an add operation is:
ConsoleMenu reads input -> ApplicationService validates -> model constructs the
object -> repository saves -> menu reports success. Main connects these objects.
The service knows only the repository interface, so a future JDBC implementation
can replace the file store without rewriting the menu's business operations.

## Run regression checks

```bash
bash run.sh --test
```

They use temporary files, not your application data. Checks cover persistence,
Unicode, duplicate validation, search, status, chronological sorting, deletion,
ID allocation, console input recovery, failed writes, and invalid saved data.
These are a small dependency-free test harness, not JUnit tests.

## Your one-hour plan

| Minutes | Activity |
| --- | --- |
| 0-10 | Extract ZIP, open terminal in project folder, run `bash run.sh` |
| 10-25 | Add two records, search, update status, filter, sort, delete one |
| 25-30 | Exit and restart; verify saved records return; run tests |
| 30-45 | Read Main, model, enum, service; trace one add operation |
| 45-60 | Read menu and repository; change one menu label and rerun |

One hour is enough to run and explore this provided version; learning to write
and explain every class independently needs additional practice.

## First demo

Choose 1, company Example Technologies, role Java Intern, Enter for today's
application date, and a deadline at least today (for example 2026-12-31 if still future).
Choose 2 to view; choose 5 and its ID to update the status; choose 7 to sort.
Choose 0 to exit and run again. Your saved entry should return under option 2.
The actual generated ID is printed when you save; use that ID rather than guessing.

## Interview preparation

1. Why an enum instead of arbitrary status strings? Restricts allowed values.
2. Why immutable models? Prevents accidental mutation of repository state.
3. Why an interface? Separates the service from a storage implementation.
4. Why LinkedHashMap? ID lookup plus predictable iteration order.
5. What is the cost of search/sort? O(n) search, O(n log n) sorting. Because each
   change copies and rewrites all records, add/update/delete are O(n) overall.
6. Why use Scanner.nextLine for numbers too? Avoids mixed nextInt/nextLine newline issues.
7. Why LocalDate? Models dates without time-zone/time-of-day confusion.
8. What would change for JDBC? SQL schema, PreparedStatement, persistence methods,
   transactions and constraints; retain service/UI contracts where practical.
9. Can this prevent duplicates across concurrent users? No; this is a single-process
   app. A database-backed version would need database constraints/concurrency handling.

Next extensions: full field editing, JDBC/PostgreSQL, JUnit, then Spring Boot REST.
Only claim the features actually implemented here when discussing this version.
