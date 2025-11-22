package org.jetbrains.plugins.template.startup

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.plugins.template.MyDaemonListener
import org.jetbrains.plugins.template.MyExecutionListener
import org.jetbrains.plugins.template.MyProcessListener
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

class MyProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        thisLogger().info("My plugin is ready.")

        val connection = project.messageBus.connect()

        connection.subscribe(
            DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
            MyDaemonListener(project)
        )

        val connection2 = project.messageBus.connect()

        connection2.subscribe(
            ExecutionManager.EXECUTION_TOPIC,
            MyExecutionListener(project)
        )
    }
}