package org.jetbrains.plugins.template

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JTextArea
import javax.swing.JPanel
import javax.swing.BorderFactory

class MyPanelFactory : ToolWindowFactory {

    companion object {
        // Static reference to the text area so we can update it later
        var textArea: JTextArea? = null
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Create panel
        val panel = JPanel()
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // Create text area
        textArea = JTextArea()
        textArea!!.isEditable = false
        textArea!!.text = "Initial text in panel.\n"

        // Optional: make it scrollable and fill the panel
        val scrollPane = JBScrollPane(textArea)
        scrollPane.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        panel.layout = BorderLayout()
        panel.add(scrollPane, BorderLayout.CENTER)

        // Add content to tool window
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    // Utility function to append text
    fun appendText(newText: String) {
        textArea?.append("\n$newText")
        textArea?.caretPosition = textArea?.document?.length ?: 0
    }
}