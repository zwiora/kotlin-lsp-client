# Kotlin LSP Client Project Explanation

## Project Purpose

This project is a simple Kotlin Language Server Protocol (LSP) client specifically designed for testing the autocompletion feature of a Kotlin LSP server. The client connects to a Kotlin LSP server running on localhost:9999, tests autocompletion functionality, and logs all communication for debugging.

## Architecture and Components

### 1. Build Configuration (build.gradle.kts)
- Uses Kotlin JVM (version 2.2.0)
- Dependencies: LSP4J (0.21.1), SLF4J (2.0.9), Logback (1.4.11)
- Configured for Java 21

### 2. Core Implementation (Main.kt)
- **KotlinLspClient class**: Implements LanguageClient interface
- **LoggingPrintWriter class**: Custom PrintWriter for logging LSP messages
- **Main function**: Connects to server, initializes it, tests autocompletion
- **setupTestProject function**: Creates test project structure
- **testAutocompletion function**: Tests autocompletion functionality

### 3. Logging Configuration (logback.xml)
- Console logging with DEBUG level
- Captures all LSP communication

## How It Works

1. **Connection**: Connects to Kotlin LSP server on localhost:9999
2. **Initialization**: Sets up client, initializes server with completion capabilities
3. **Test Project Setup**: Creates basic project with test Kotlin file
4. **Autocompletion Testing**: Sends completion request, displays results
5. **Shutdown**: Properly terminates connection

## How to Run

1. **Prerequisites**: Java 21, Kotlin LSP server on localhost:9999
2. **Build and Run**:
   ```bash
   ./gradlew build
   ./gradlew run
   ```

This client is useful for testing LSP server autocompletion, learning about the LSP protocol, and diagnosing issues with Kotlin code completion in a controlled environment.