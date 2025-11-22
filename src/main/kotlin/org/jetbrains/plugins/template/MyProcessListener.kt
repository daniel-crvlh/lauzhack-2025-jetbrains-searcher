package org.jetbrains.plugins.template;

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.util.Key

class MyProcessListener : ProcessAdapter() {

    val emptyStringArray = arrayOf<String>()

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val line = event.text
        
        if (line.contains("Exception") || line.contains("ERROR")) {
            println("🔥 RUNTIME ERROR: $line")
        }
    }
}