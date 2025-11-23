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

                        ApplicationManager.getApplication().executeOnPooledThread {
                            try {
                                val url = URL("https://dogapi.dog/api/v2/facts?limit=1")
                                val conn = url.openConnection() as HttpURLConnection
                                conn.requestMethod = "GET"
                                conn.connectTimeout = 3000
                                conn.readTimeout = 3000

                                val response = conn.inputStream.bufferedReader().readText()

                                val responseObj = Json.decodeFromString<ApiResponse
                                    .ApiResponse>("{ \"data\": \"DATADATA\" }")

                                val fact = responseObj.data

                                // Show popup in UI thread
                                ApplicationManager.getApplication().invokeLater {
                                    showEditorHint(
                                        editor.editor,
                                        "🐶 Fun fact: $fact"
                                    )

                                    MyPanelFactory.textArea?.append("${error.description}\n")

                                    MyPanelFactory.textArea?.append("$fact\n\n")
                                }

                            } catch (e: Exception) {
                                println("❌ Failed to fetch dog fact: ${e.message}")
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
