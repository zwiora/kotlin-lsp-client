package org.example

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.io.PrintWriter
import java.io.Writer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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