Placement Application Tracker
A console-based Java application to manage and track job and internship applications in one place. This project was built to practice core Java, OOP, collections, exception handling, and file storage.
Current Features
- Add applications with company name, role, application date, and deadline.
- View all applications or find an application by ID.
- Search applications by company name or role.
- Update application status: Applied, Assessment, Interview, Offered, or Rejected.
- Delete applications with confirmation.
- Sort applications by deadline and filter them by status.
- View a summary of application counts by status.
- Validate input and prevent duplicate company-role-date entries.
- Save records to a local file and reload them when the app restarts.
Technologies Used
- Java 17+ for application logic.
- OOP and interfaces to separate the model, business logic, storage, and console menu.
- Java Collections to manage application records.
- Java File I/O and Properties for local persistence.
- LocalDate for date handling and validation.
Project Structure
Location	Purpose
src/com/ashish/placementtracker/Main.java	Application entry point
src/com/ashish/placementtracker/model/	Application model and status enum
src/com/ashish/placementtracker/service/	Business logic and validation
src/com/ashish/placementtracker/repository/	Storage interface and file implementation
src/com/ashish/placementtracker/ui/	Console menu and input handling
test/	Automated regression checks
data/	Saved application records
run.sh	Compile and run script


How to Run
Install JDK 17 or newer, then open a terminal in the project folder:
bash run.sh
The launcher also supports the Android Studio JDK installed at $HOME/Applications/android-studio/jbr. To select it explicitly:
JAVA_HOME="$HOME/Applications/android-studio/jbr" bash run.sh
To run the automated checks:
bash run.sh --test
Records are saved automatically to data/applications.properties. This version uses local file storage and should be run as one instance at a time.
Future Improvements
The following features are planned and are not part of the current version:
1. SQL database: Replace file storage with PostgreSQL or MySQL using JDBC.
2. Spring Boot: Expose application operations through REST APIs.
3. Spring Data JPA: Simplify database access and entity mapping.
4. Authentication and authorization: Add user accounts and protect access to each user's records using Spring Security.
5. Web interface: Build a frontend to manage applications through a browser.
6. Deadline reminders: Add email notifications for upcoming deadlines.
7. Advanced tracking: Add interview schedules, notes, and document links.
8. Search and reporting: Add pagination, dashboard charts, and CSV export.
9. Testing and deployment: Add JUnit tests, API integration tests, Docker packaging, and CI checks.
Learning Goal
Start with a working core Java application, understand its design, and gradually extend it into a database-backed Spring Boot application.