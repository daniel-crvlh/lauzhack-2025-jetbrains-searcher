package org.jetbrains.plugins.template

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JEditorPane
import javax.swing.JPanel

class MyPanelFactory : ToolWindowFactory {

    companion object {
        // Static reference so listeners can update it
        var editorPane: JEditorPane? = null

        fun appendText(html: String) {
            val pane = editorPane ?: return
            val old = pane.text

            // Keep HTML root, append inside <body>
            val newHtml =
                old.replace(
                    "</body>",
                    "$html<br></body>"
                )

            pane.text = newHtml
            pane.caretPosition = pane.document.length
        }

        /** Clears panel content */
        fun resetPanel() {
            editorPane?.text = """
                <html>
                    <body style="font-family: monospace; font-size: 13px;">
                        <h2>Syntax Helper</h2>
                        <hr>
                    </body>
                </html>
            """.trimIndent()
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Root panel
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // HTML-enabled editor pane
        val htmlPane = JEditorPane().apply {
            contentType = "text/html"
            isEditable = false
            text = """
                <html>
                    <body style="font-family: monospace; font-size: 13px;">
                        <h2>Syntax Helper</h2>
                        <hr>
                    </body>
                </html>
            """.trimIndent()
        }

        // Scrollable view
        val scrollPane = JBScrollPane(htmlPane)

        // Add components
        panel.add(scrollPane, BorderLayout.CENTER)

        // Add to ToolWindow
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        // Save reference globally
        editorPane = htmlPane
    }
}
