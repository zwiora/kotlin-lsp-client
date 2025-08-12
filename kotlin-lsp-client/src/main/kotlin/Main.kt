package org.example

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.net.Socket
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * A simple LSP client for testing Kotlin LSP autocompletion feature.
 * Connects to a Kotlin LSP server running on localhost:9999.
 */
class KotlinLspClient : LanguageClient {
    override fun telemetryEvent(obj: Any?) {
        println("[DEBUG] Telemetry event: $obj")
    }

    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {
        println("[DEBUG] Received diagnostics: $diagnostics")
    }

    override fun showMessage(messageParams: MessageParams?) {
        println("[INFO] Server message: ${messageParams?.message}")
    }

    override fun showMessageRequest(requestParams: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> {
        println("[INFO] Server message request: ${requestParams?.message}")
        return CompletableFuture.completedFuture(null)
    }

    override fun logMessage(message: MessageParams?) {
        println("[DEBUG] Server log: ${message?.message}")
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
        val launcher = LSPLauncher.Builder<LanguageServer>()
            .setLocalService(client)
            .setRemoteInterface(LanguageServer::class.java)
            .setInput(socket.getInputStream())
            .setOutput(socket.getOutputStream())
            .create()

        // Start listening for incoming messages
        val listening = launcher.startListening()
        
        // Get the remote proxy
        val server = launcher.remoteProxy
        
        // Initialize the server
        val initParams = InitializeParams()
        initParams.processId = ProcessHandle.current().pid().toInt()
        val currentpath = System.getProperty("user.dir")
        initParams.workspaceFolders = listOf(WorkspaceFolder(Paths.get(currentpath).parent.resolve("basic-project").toUri().toString(), "basic kotlin project"))
        
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

        return

        // Notify that initialization is complete
        server.initialized(InitializedParams())
        
        // Create the basic-project directory and a sample Kotlin file
        try {
            setupTestProject()
        }catch(e: Exception) {
            println("[ERROR] Error setting up test project: ${e.message}")
            e.printStackTrace()
            return
        }
        
        // Test autocompletion
//        testAutocompletion(server)
        
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
 * Sets up a basic test project structure
 */
fun setupTestProject() {
    println("[INFO] Setting up test project...")
    
    val projectDir = File("../basic-project")
    if (!projectDir.exists()) {
        println("[ERROR] Test project directory ../basic-project does not exist.")
        throw RuntimeException("Test project directory does not exist.")
    }
    
    val srcDir = File(projectDir, "src")
    if (!srcDir.exists()) {
        println("[ERROR] Test project source directory src does not exist.")
        throw RuntimeException("Test project source directory does not exist.")
    }
    
    val testFile = File(srcDir, "Test.kt")
    if (!testFile.exists()) {
        println("[ERROR] Test project source file Test.kt does not exist.")
        throw RuntimeException("Test project source file does not exist.")
    }
}

/**
 * Tests autocompletion functionality
 */
fun testAutocompletion(server: LanguageServer) {
    println("[INFO] Testing autocompletion...")
    
    // Create a completion request
    val completionParams = CompletionParams()
    completionParams.textDocument = TextDocumentIdentifier(
        Paths.get("basic-project/src/Test.kt").toUri().toString()
    )
    
    // Position after "test." in the file
    completionParams.position = Position(2, 9)
    
    try {
        // Send completion request
        println("[INFO] Sending completion request for position (2,9)...")
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
            println("[INFO] ${index + 1}. ${item.label ?: "No label"} - ${item.detail ?: "No detail"}")
        }
    } catch (e: Exception) {
        println("[ERROR] Error getting completions: ${e.message}")
        e.printStackTrace()
    }
}