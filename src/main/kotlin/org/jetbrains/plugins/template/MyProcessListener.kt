package org.jetbrains.plugins.template;

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.util.Key

class MyProcessListener : ProcessAdapter() {

    val entrys: MutableList<String> = mutableListOf()

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val line = event.text

        if (line.contains("Exception") || line.contains("ERROR")) {
            if(entrys.isNotEmpty()) {
                var last_elem = entrys.last()
                if (line != last_elem) {
                    entrys.add(last_elem)
                    println("🔥 RUNTIME ERROR: $line")
                }

            } else {
                entrys.add(line)
                println("🔥 RUNTIME ERROR: $line")
            }
        }

    }
}