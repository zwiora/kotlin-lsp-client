package org.example

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.net.Socket
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


fun main() {
    // Parameters for running the client
    val port = 9999
    val testProjectPath = Paths.get(System.getProperty("user.dir")).parent.resolve("basic-project")
    val testFile = "src/main/kotlin/Main.kt"
    val testFileFull = "../basic-project/$testFile"

    println("[INFO] Starting Kotlin LSP client...\n")
    val uri = testProjectPath.resolve(testFile).toUri()
        .toString()
    val text = File(testFileFull).readText()

    try {
        // Connect to the Kotlin LSP server
        println("[INFO] Connecting to Kotlin LSP server at localhost:$port...")
        val server = connectToLSPServer(port) ?: throw RuntimeException("Failed to connect to Kotlin LSP server")

        println("[INFO] Sending initialize request...")
        server.initialize(generateInitParams(testProjectPath.toUri().toString())).get(30, TimeUnit.SECONDS)

        println("[INFO] Sending initialized notification...")
        server.initialized(InitializedParams())

        println("[INFO] Sending didOpen notification...")
        server.textDocumentService.didOpen(generateDidOpenParams(uri, text))

        println("[INFO] Sending completion request...")
        var completionResult = server.textDocumentService.completion(generateCompletionParams(uri)).get(5, TimeUnit.SECONDS)

        // Handle the Either<List<CompletionItem>, CompletionList> result
        var items = when {
            completionResult.isLeft -> completionResult.left
            completionResult.isRight -> completionResult.right.items
            else -> emptyList()
        }

        // Display results
        println("[INFO] Received ${items.size} completion items:")
        items.forEachIndexed { index: Int, item: CompletionItem ->
            println("${index + 1}. ${item.label ?: "No label"} - ${item.documentation ?: "No documentation"}")
        }
        println()

        println("[INFO] Sending didChange notification...")
        server.textDocumentService.didChange(generateDidChangeParams(uri))

        println("[INFO] Sending completion request...")
        completionResult = server.textDocumentService.completion(generateCompletionParams(uri)).get(5, TimeUnit.SECONDS)

        // Handle the Either<List<CompletionItem>, CompletionList> result
        items = when {
            completionResult.isLeft -> completionResult.left
            completionResult.isRight -> completionResult.right.items
            else -> emptyList()
        }

        // Display results
        println("[INFO] Received ${items.size} completion items:")
        items.forEachIndexed { index: Int, item: CompletionItem ->
            println("${index + 1}. ${item.label ?: "No label"} - ${item.documentation ?: "No documentation"}")
        }
        println()

        // Resolve the first completion item
        if (items.isNotEmpty()) {
            println("[INFO] Resolving first completion item: ${items[0].label}")
            val resolvedItem = server.textDocumentService.resolveCompletionItem(items[0]).get(5, TimeUnit.SECONDS)

            // Display result details
            println("[INFO] Resolved item details:")
            println("Label: ${resolvedItem.label}")
            println("Detail: ${resolvedItem.detail}")
            println("Documentation: ${resolvedItem.documentation}")
            println("Additional data: ${resolvedItem.data}")
        }

        println("[INFO] Sending didClose notification...")
        server.textDocumentService.didClose(DidCloseTextDocumentParams(TextDocumentIdentifier(uri)))
        
        println("[INFO] Shutting down server...")
        server.shutdown().get(5, TimeUnit.SECONDS)
        server.exit()

        println("[INFO] LSP client terminated successfully")
    } catch (e: Exception) {
        println("[ERROR] Error in LSP client: ${e.message}")
        e.printStackTrace()
    }
}

private fun generateCompletionParams(uri: String): CompletionParams {
    val completionParams = CompletionParams()
    completionParams.textDocument = TextDocumentIdentifier(
        uri
    )
    completionParams.position = Position(6, 10)
    return completionParams
}

private fun generateDidChangeParams(uri: String): DidChangeTextDocumentParams {
    val didChangeParams = DidChangeTextDocumentParams(
        VersionedTextDocumentIdentifier(uri, 2),
        listOf(
            TextDocumentContentChangeEvent(
                Range(
                    Position(6, 0),
                    Position(7, 0)
                ),
                "printl\n"
            )
        )
    )
    return didChangeParams
}

private fun generateDidOpenParams(uri : String, text :String): DidOpenTextDocumentParams {

    val didOpenParams = DidOpenTextDocumentParams(
        TextDocumentItem(
            uri,
            "kotlin",
            1,
            text
        )
    )
    return didOpenParams
}

private fun generateInitParams(uri: String): InitializeParams {
    val initParams = InitializeParams()
    initParams.processId = ProcessHandle.current().pid().toInt()
    initParams.workspaceFolders = listOf(
        WorkspaceFolder(
            uri,
            "basic kotlin project"
        )
    )

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
    return initParams
}

private fun connectToLSPServer(@Suppress("SameParameterValue") port : Int): LanguageServer? {
    val socket = Socket("localhost", port)
    println("[INFO] Connected to Kotlin LSP server at localhost:9999")

    // Create the client instance
    val client = KotlinLspClient()

    // Create the launcher with message tracing enabled
    println("[INFO] Message tracing enabled - all sent and received messages will be displayed")
    val messageLogger = LoggingPrintWriter(System.out.writer())
    val launcher = LSPLauncher.Builder<LanguageServer>()
        .setLocalService(client)
        .setRemoteInterface(LanguageServer::class.java)
        .setInput(socket.getInputStream())
        .setOutput(socket.getOutputStream())
        .traceMessages(messageLogger)
        .create()

    // Start listening for incoming messages
    launcher.startListening()

    // Get the remote proxy
    val server = launcher.remoteProxy
    return server
}