# maven-build Specification

## Purpose
Defines how the project is built: one Maven module whose `pom.xml` states the Java and Maven versions it needs and targets Java 21.

## Requirements

### Requirement: Build with Maven
Running `mvn verify` at the repository root with a JDK 21 or newer and Maven 3.9 or newer SHALL compile the application and run its tests.

#### Scenario: Clean build
- **WHEN** `mvn verify` is run at the repository root with JDK 21 and Maven 3.9
- **THEN** the build succeeds and the tests pass

### Requirement: Single module
The project SHALL be one Maven module with one `pom.xml` at the repository root and no sub-modules.

#### Scenario: One module in the build
- **WHEN** the build starts
- **THEN** the reactor contains exactly one project

### Requirement: Declared Java and Maven requirements
The `pom.xml` SHALL declare that the build needs Java 21 or newer and Maven 3.9 or newer, and the build SHALL fail before compiling anything, with a message naming the unmet requirement, when either is not met.

#### Scenario: Older JDK
- **WHEN** `mvn verify` is run with a JDK older than 21
- **THEN** the build fails before compiling, with a message saying Java 21 or newer is required

### Requirement: Java 21 target
The application SHALL compile to Java 21 bytecode, also when built with a newer JDK.

#### Scenario: Built with a newer JDK
- **WHEN** the project is built with a JDK newer than 21
- **THEN** the compiled classes report class-file major version 65 (Java 21)
