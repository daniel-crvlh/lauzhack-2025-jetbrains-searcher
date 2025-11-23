package org.jetbrains.plugins.template

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project

class MyExecutionListener(private val project: Project) : ExecutionListener {

    override fun processNotStarted(executorId: String, env: ExecutionEnvironment) {

    }

    override fun processStarting(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        println("⚡ Process is starting: $executorId")
        handler.addProcessListener(MyProcessListener())
        // Cannot attach listener here — handler not yet created
    }

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        println("▶️ Process started: attaching listener")
        //handler.addProcessListener(MyProcessListener())
    }
}