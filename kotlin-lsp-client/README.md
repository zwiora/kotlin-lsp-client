# Kotlin LSP Client

A simple Language Server Protocol (LSP) client implementation for interacting with a Kotlin Language Server. This project demonstrates how to connect to a Kotlin LSP server and use its code completion capabilities.

The main project can be found in the kotlin-lsp-client folder. The basic-project folder contains a simple, auto-generated project used for testing.

## Overview

This client connects to a Kotlin Language Server running on localhost:9999 and demonstrates:
- Establishing a connection to the server
- Initializing the LSP session with appropriate capabilities
- Opening a Kotlin file for editing
- Requesting and displaying code completions
- Resolving completion items for more detailed information
- Properly shutting down the connection

## Prerequisites

- JDK 17 or higher
- Gradle 7.0 or higher
- A running Kotlin Language Server on localhost:9999

## Dependencies

- [LSP4J](https://github.com/eclipse/lsp4j) - Eclipse's implementation of the Language Server Protocol
- [Gson](https://github.com/google/gson) - For JSON parsing and formatting

## Building the Project

```bash
./gradlew build
```

## Running the Client

```bash
./gradlew run
```

The client will:
1. Connect to the Kotlin LSP server at localhost:9999
2. Open a Kotlin file from the `basic-project` directory
3. Request code completions
4. Display the completion results
5. Resolve and display detailed information for the first completion item
6. Close the connection

## Project Structure

- `src/main/kotlin/Main.kt` - The entry point of the application, containing the main logic for connecting to and interacting with the LSP server
- `src/main/kotlin/KotlinLspClient.kt` - Implementation of the LanguageClient interface from LSP4J
- `src/main/kotlin/LoggingPrintWriter.kt` - Utility class for logging LSP messages

## Usage Example

The client is configured to open a specific Kotlin file from a `basic-project` directory and request completions at a specific position. You can modify the file path and position in the `Main.kt` file to test different scenarios.

## Notes

- This client is designed for demonstration purposes and may require adjustments for production use
- Make sure the Kotlin Language Server is running on port 9999 before starting the client
- The client enables message tracing, so all communication between the client and server will be displayed in the console
