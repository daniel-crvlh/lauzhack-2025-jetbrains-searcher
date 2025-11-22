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
                println("🔥 Errors found in ${psiFile.name} (${errors.size})")

                errors.forEach {
                    println(" - ${it.description}")
                }
            }
        }
    }
}
