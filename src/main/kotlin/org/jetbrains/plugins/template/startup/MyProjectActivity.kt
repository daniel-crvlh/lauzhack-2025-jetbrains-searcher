package org.jetbrains.plugins.template.startup

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.plugins.template.MyDaemonListener
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

        val runManager = RunManager.getInstance(project)
        val settings = runManager.selectedConfiguration ?: return
        val executor = DefaultRunExecutor.getRunExecutorInstance()

        // This executes the configuration
        ProgramRunnerUtil.executeConfiguration(settings, executor)
    }
}