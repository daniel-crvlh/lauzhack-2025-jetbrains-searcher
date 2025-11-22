package org.jetbrains.plugins.template

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiFile

class MyDaemonListener(private val project: Project) : DaemonCodeAnalyzer.DaemonListener {

    val entries: MutableList<String> = mutableListOf()

    override fun daemonFinished(fileEditors: Collection<FileEditor>) {
        handleEditors(fileEditors)
    }

    override fun daemonFinished() {
        val editors = FileEditorManager.getInstance(project).allEditors.toList()
        handleEditors(editors)
    }

    private fun handleEditors(editors: Collection<FileEditor>) {
        val psiManager = PsiManager.getInstance(project)

        for (editor in editors) {
            val vf = editor.file ?: continue
            val psiFile = psiManager.findFile(vf) ?: continue

            val document = FileEditorManager.getInstance(project)
                .selectedTextEditor
                ?.document ?: continue

            // Get HighlightInfo (errors, warnings, etc.)
            val errors = DaemonCodeAnalyzerImpl
                .getHighlights(document, null, project)
                .filter { it.severity.myName == "ERROR" }

            if (errors.isNotEmpty()) {

                errors.forEach { error ->
                    if (entries.isNotEmpty()) {
                        val lastElem = entries.last()
                        if (error.description != lastElem) {
                            entries.add(error.description)
                            println("🔥 SYNTAX ERROR: ${error.description}")
                        }
                    } else {
                        entries.add(error.description)
                        println("🔥 SYNTAX ERROR: ${error.description}")
                    }
                }
            }
        }
    }
}
