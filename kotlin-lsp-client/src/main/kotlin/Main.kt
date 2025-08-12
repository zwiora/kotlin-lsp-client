package org.example

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.io.PrintWriter
import java.io.Writer
import java.net.Socket
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Custom PrintWriter for logging LSP messages.
 * Formats and displays the content of all sent and received messages.
 * Properly formats JSON content to make it more readable.
 */
class LoggingPrintWriter(writer: Writer) : PrintWriter(writer, true) {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    override fun println(message: String) {
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val formattedContent = formatJsonIfPossible(message)
        val formattedMessage = "[$timestamp] LSP Message: $formattedContent"
        super.println(formattedMessage)
        super.flush()
    }

    override fun println(obj: Any?) {
        println(obj.toString())
    }

    /**
     * Attempts to format the message as JSON if it appears to be JSON content.
     * If the message is not valid JSON or doesn't look like JSON, returns the original message.
     */
    private fun formatJsonIfPossible(message: String): String {
        val trimmed = message.trim()
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                // Parse the JSON string to a JsonElement
                val jsonElement = JsonParser.parseString(trimmed)
                // Convert back to a properly formatted JSON string
                return gson.toJson(jsonElement)
            } catch (_: JsonSyntaxException) {
                // If parsing fails, return the original message
                return message
            }
        }
        return message
    }
}

/**
 * A simple LSP client for testing Kotlin LSP autocompletion feature.
 * Connects to a Kotlin LSP server running on localhost:9999.
 */
class KotlinLspClient : LanguageClient {
    override fun telemetryEvent(obj: Any?) {
        println("[SERVER LOG] Telemetry event: $obj")
    }

    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {
        println("[SERVER LOG] Received diagnostics: $diagnostics")
    }

    override fun showMessage(messageParams: MessageParams?) {
        println("[SERVER LOG] Server message: ${messageParams?.message}")
    }

    override fun showMessageRequest(requestParams: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> {
        println("[SERVER LOG] Server message request: ${requestParams?.message}")
        return CompletableFuture.completedFuture(null)
    }

    override fun logMessage(message: MessageParams?) {
        println("[SERVER LOG] Server log: ${message?.message}")
    }
}

fun main() {
    println("[INFO] Starting Kotlin LSP client...")

    try {
        // Connect to the Kotlin LSP server running on localhost:9999
        val socket = Socket("localhost", 9999)
        println("[INFO] Connected to Kotlin LSP server at localhost:9999")

        // Create the client instance
        val client = KotlinLspClient()

        // Create the launcher with message tracing enabled
        val messageLogger = LoggingPrintWriter(System.out.writer())
        println("[INFO] Message tracing enabled - all sent and received messages will be displayed")

        val launcher = LSPLauncher.Builder<LanguageServer>()
            .setLocalService(client)
            .setRemoteInterface(LanguageServer::class.java)
            .setInput(socket.getInputStream())
            .setOutput(socket.getOutputStream())
            .traceMessages(messageLogger)
            .create()

        // Start listening for incoming messages
        val listening = launcher.startListening()
        
        // Get the remote proxy
        val server = launcher.remoteProxy
        
        // Initialize the server
        val initParams = InitializeParams()
        initParams.processId = ProcessHandle.current().pid().toInt()
        val currentPath = System.getProperty("user.dir")
        initParams.workspaceFolders = listOf(WorkspaceFolder(Paths.get(currentPath).parent.resolve("basic-project").toUri().toString(), "basic kotlin project"))
        
        // Set capabilities
        val capabilities = ClientCapabilities()
        val textDocumentClientCapabilities = TextDocumentClientCapabilities()
        
        // Set completion capabilities
        val completionCapabilities = CompletionCapabilities()
        completionCapabilities.dynamicRegistration = true
        completionCapabilities.completionItem = CompletionItemCapabilities()
        completionCapabilities.completionItem.snippetSupport = true
        completionCapabilities.completionItem.documentationFormat = listOf(MarkupKind.MARKDOWN)
        textDocumentClientCapabilities.completion = completionCapabilities
        
        capabilities.textDocument = textDocumentClientCapabilities
        initParams.capabilities = capabilities
        initParams.clientInfo = ClientInfo("KotlinLspClient", "1.0.0")
        
        println("[INFO] Initializing LSP server...")
        val initResult = server.initialize(initParams).get(60, TimeUnit.SECONDS)
        println("[INFO] Server initialized with capabilities: ${initResult.capabilities}")


        // Notify that initialization is complete
        println("[INFO] Sending initialized notification...")
        server.initialized(InitializedParams())

        
        // Test autocompletion
        testAutocompletion(server)
        
        // Shutdown the server
        println("[INFO] Shutting down server...")
        server.shutdown().get(5, TimeUnit.SECONDS)
        server.exit()
        
        // Wait for the listening thread to complete
        listening.get(5, TimeUnit.SECONDS)
        
        println("[INFO] LSP client terminated successfully")
    } catch (e: Exception) {
        println("[ERROR] Error in LSP client: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Tests autocompletion functionality
 */
fun testAutocompletion(server: LanguageServer) {
    println("[INFO] Testing autocompletion...")
    val uri = Paths.get(System.getProperty("user.dir")).parent.resolve("basic-project/src/main/kotlin/Main.kt").toUri().toString()
    val text = File("../basic-project/src/main/kotlin/Main.kt").readText()

    // Send didOpen notification
    val didOpenParams = DidOpenTextDocumentParams(
        TextDocumentItem(
            uri,
            "kotlin",
            1,
            text
        )
    )

    println("[INFO] Sending didOpen notification...")
    server.textDocumentService.didOpen(didOpenParams)


    // Send didChange notification
    val didChangeParams = DidChangeTextDocumentParams(
        VersionedTextDocumentIdentifier(uri, 2),
        listOf(
            TextDocumentContentChangeEvent(
                Range(
                    Position(6, 0),
                    Position(6, text.lines()[6].length)
                ),
                "printl"
            )
        )
    )
    server.textDocumentService.didChange(didChangeParams)

    // Create a completion request
    val completionParams = CompletionParams()
    completionParams.textDocument = TextDocumentIdentifier(
        uri
    )

    // Position after "printl" in the file
    completionParams.position = Position(6, 10)

    try {
        // Send completion request
        println("[INFO] Sending completion request for position (6,10)...")
        val completionResult = server.textDocumentService.completion(completionParams).get(5, TimeUnit.SECONDS)

        // Handle the Either<List<CompletionItem>, CompletionList> result
        val items = when {
            completionResult.isLeft -> completionResult.left
            completionResult.isRight -> completionResult.right.items
            else -> emptyList()
        }

        // Display results
        println("[INFO] Received ${items.size} completion items:")
        items.forEachIndexed { index: Int, item: CompletionItem ->
            println("${index + 1}. ${item.label ?: "No label"} - ${item.documentation ?: "No detail"}")
        }
        println()

        // Resolve the first completion item
        if (items.isNotEmpty()) {
            println("[INFO] Resolving first completion item: ${items[0].label}")
            val resolvedItem = server.textDocumentService.resolveCompletionItem(items[0]).get(5, TimeUnit.SECONDS)
            println("[INFO] Resolved item details:")
            println("Label: ${resolvedItem.label}")
            println("Detail: ${resolvedItem.detail}")
            println("Documentation: ${resolvedItem.documentation}")
            println("Additional data: ${resolvedItem.data}")
        }

    } catch (e: Exception) {
        println("[ERROR] Error getting completions: ${e.message}")
        e.printStackTrace()
    }

    server.textDocumentService.didClose(DidCloseTextDocumentParams(TextDocumentIdentifier(uri)))
}