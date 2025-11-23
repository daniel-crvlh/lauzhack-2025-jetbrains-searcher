package org.jetbrains.plugins.template

import ai.grazie.utils.json.JSONObject
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.psi.PsiManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class MyDaemonListener(private val project: Project) : DaemonCodeAnalyzer.DaemonListener {

    var entries: List<HighlightInfo> = listOf()

    override fun daemonFinished(fileEditors: Collection<FileEditor>) {
        handleEditors(fileEditors)
    }

    override fun daemonFinished() {
        val editors = FileEditorManager.getInstance(project).allEditors.toList()
        handleEditors(editors)
    }

    fun showEditorHint(editor: Editor, message: String) {
        HintManager.getInstance().showInformationHint(editor, message)
    }

    private fun handleEditors(editors: Collection<FileEditor>) {
        val psiManager = PsiManager.getInstance(project)

        for (fileEditor in editors) {
            val vf = fileEditor.file ?: continue
            val psiFile = psiManager.findFile(vf) ?: continue

            // Only handle text editors
            val editor = fileEditor as? TextEditor ?: continue
            val document = editor.editor.document

            // Get HighlightInfo (errors, warnings, etc.)
            val errors = DaemonCodeAnalyzerImpl
                .getHighlights(document, null, project)
                .filter { it.severity.myName == "ERROR" }

            if (errors.isNotEmpty()) {

                errors.forEach { error ->
                    if (!entries.contains(error)) {
                        MyPanelFactory.appendText("🔥 SYNTAX ERROR AND LOADING HELP...\n")

                        ApplicationManager.getApplication().executeOnPooledThread {
                            try {
                                val url = URL(" http://127.0.0.1:8000/predict")
                                val conn = url.openConnection() as HttpURLConnection
                                conn.requestMethod = "POST"
                                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                                conn.setRequestProperty("Accept", "application/json")
                                conn.connectTimeout = 20000
                                conn.readTimeout = 20000
                                conn.doOutput = true // Important for POST

                                var error_api = error.description

                                val jsonBody: String = "{\n" +
                                        "\"error\":\"$error_api\",\n" +
                                        "\"code\": \"\",\n" +
                                        "\"lineNb\":12,\n" +
                                        "\"function\":\"syntax\",\n" +
                                        "\"language\":\"java\"\n" +
                                        "}"

                                // Write body
                                conn.outputStream.use { os ->
                                    val input = jsonBody.toByteArray(Charsets.UTF_8)
                                    os.write(input, 0, input.size)
                                }

                                val response = conn.inputStream.bufferedReader().readText()

                                val responseObj = Json.decodeFromString<ApiResponse
                                    .ApiResponse>(response)

                                val code = responseObj.code
                                val explanation = responseObj.explanation
                                val short_description = responseObj.shortDescription

                                // Show popup in UI thread
                                ApplicationManager.getApplication().invokeLater {
                                    showEditorHint(
                                        editor.editor,
                                        short_description
                                    )

                                    MyPanelFactory.appendText("Your code has an error : \n")
                                    MyPanelFactory.appendText("${error.description}\n\n")

                                    MyPanelFactory.appendText("Some explanation : \n")
                                    MyPanelFactory.appendText("$explanation\n\n")

                                    MyPanelFactory.appendText("A snippet of code to help you : \n")
                                    MyPanelFactory.appendText("$code\n\n")

                                    MyPanelFactory.appendText("-----------------------------------\n\n")
                                }

                            } catch (e: Exception) {
                                println("❌ Failed to fetch data : ${e.message}")
                            }
                        }

                        println("🔥 SYNTAX ERROR AND LOADING HELP...")
                    }
                }

                entries = errors
            }
        }
    }
}
