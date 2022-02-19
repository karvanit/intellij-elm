package org.elm.ide.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.impl.ContentImpl
import com.intellij.util.ui.MessageCategory
import org.elm.openapiext.findFileByPath
import org.elm.workspace.compiler.ElmBuildAction
import org.elm.workspace.compiler.ElmError
import java.nio.file.Path

class ElmCompilerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        with(project.messageBus.connect()) {
            subscribe(ElmBuildAction.ERRORS_TOPIC, object : ElmBuildAction.ElmErrorsListener {
                override fun update(baseDirPath: Path, messages: List<ElmError>, targetPath: String, offset: Int) {

                    val errorTreeViewPanel = ElmErrorTreeViewPanel(project, "Elm Compiler", createExitAction = false, createToolbar = true)
                    connectFriendlyMessages(project, errorTreeViewPanel)

                    for (message in messages) {
                        val sourceLocation = message.location!!
                        val virtualFile = baseDirPath.resolve(sourceLocation.path).let {
                            LocalFileSystem.getInstance().findFileByPath(it)
                        }
                        errorTreeViewPanel.addMessage(
                            MessageCategory.ERROR, arrayOf(message.title),
                            virtualFile,
                            sourceLocation.region?.start?.let { it.line - 1 } ?: 0,
                            sourceLocation.region?.start?.let { it.column - 1 } ?: 0,
                            message.html
                        )
                    }
                    toolWindow.contentManager.removeAllContents(true)
                    toolWindow.contentManager.addContent(ContentImpl(errorTreeViewPanel, "Compilation result", true))
                    toolWindow.activate(null)
                }
            })
        }
    }
}
