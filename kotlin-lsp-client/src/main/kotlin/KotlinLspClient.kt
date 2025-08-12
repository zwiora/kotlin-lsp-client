package org.example

import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture

/**
 * A simple LSP client for testing Kotlin LSP autocompletion feature.
 * Connects to a Kotlin LSP server running on localhost:9999.
 */
class KotlinLspClient : LanguageClient {

    override fun telemetryEvent(obj: Any?) {
        println("[SERVER] Telemetry event: $obj")
    }

    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {
        println("[SERVER] Received diagnostics: $diagnostics")
    }

    override fun showMessage(messageParams: MessageParams?) {
        println("[SERVER] Server message: ${messageParams?.message}")
    }

    override fun showMessageRequest(requestParams: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> {
        println("[SERVER] Server message request: ${requestParams?.message}")
        return CompletableFuture.completedFuture(null)
    }

    override fun logMessage(message: MessageParams?) {
        println("[SERVER] Server log: ${message?.message}")
    }
}