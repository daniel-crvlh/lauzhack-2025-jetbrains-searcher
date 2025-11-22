package org.jetbrains.plugins.template

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiFile

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
                        println("🔥 SYNTAX ERROR: ${error.description}")
                        showEditorHint(editor.editor, "Syntax Error: ${error.description}")
                    }
                }

                entries = errors
            }
        }
    }
}
