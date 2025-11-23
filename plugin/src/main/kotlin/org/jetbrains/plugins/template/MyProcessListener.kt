package org.jetbrains.plugins.template;

import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key
import com.intellij.util.asSafely
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MyProcessListener : ProcessAdapter() {

    var newException: Boolean = false
    var exceptionMessage: String = ""

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val line = event.text

        if (line.startsWith("Exception in thread")) {
            newException = true
            exceptionMessage = line
        }

        val regex = Regex("at [\\w\\d]+\\.([\\w\\d]+)\\(([\\w\\d]+\\.[\\w\\d]+):(\\d+)\\)")
        val matches = regex.find(line)
        if (matches != null && matches.groups.count() == 4 && newException) {
            newException = false
            val methodName = matches.groups[1]?.value
            val filename = matches.groups[2]?.value
            val lineNumber = matches.groups[3]?.value

            val args = event.source.asSafely<KillableColoredProcessHandler>()?.commandLine?.split(' ')
            if (args != null && args.isNotEmpty()) {
                val dirName = args[args.indexOf("-classpath") + 1]

                val path = File(dirName.substringBefore("\\out"))
                val filepath = path.walk().firstOrNull { it.name == filename }

                if (filepath != null) {
                    var fileContents = ""
                    filepath.readLines().forEach {
                        fileContents += it.replace("\"", "\\\"") + " "
                    }

                    val jsonToSend = """
                    {
                        "error":"${exceptionMessage.replace("\"", "\\\"")}",
                        "code":"$fileContents",
                        "lineNb":$lineNumber,
                        "function":"$methodName",
                        "language":"java"
                    }
                    """.trimIndent()

                    MyPanelFactory.textArea?.append("$jsonToSend\n\n")

                    ApplicationManager.getApplication().executeOnPooledThread {
                        try {
                            val url = URL("http://127.0.0.1:8000/predict")
                            val conn = url.openConnection() as HttpURLConnection
                            conn.requestMethod = "POST"
                            conn.connectTimeout = 3000
                            conn.readTimeout = 30000
                            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                            conn.setRequestProperty("Accept", "application/json")
                            conn.doOutput = true

                            conn.outputStream.write(jsonToSend.toByteArray())

                            val response = conn.inputStream.bufferedReader().readText()

                            val responseObj = Json.decodeFromString<ApiResponse
                                .ApiResponse>(response)

                            val code = responseObj.code
                            val explanation = responseObj.explanation

                            // Show popup in UI thread
                            ApplicationManager.getApplication().invokeLater {
                                MyPanelFactory.textArea?.append("$exceptionMessage\n")

                                MyPanelFactory.textArea?.append("$explanation\n")
                                MyPanelFactory.textArea?.append("$code\n\n")
                            }

                        } catch (e: Exception) {
                            println("❌ Failed to fetch dog fact: ${e.message}")
                        }
                    }
                }

                //println("🔥 RUNTIME ERROR: $line")
            }
        }

        if (line.contains("Exception") || line.contains("ERROR")) {



        }

    }
}