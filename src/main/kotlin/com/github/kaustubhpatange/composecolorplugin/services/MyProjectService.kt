package com.github.kaustubhpatange.composecolorplugin.services

import com.intellij.openapi.project.Project
import com.github.kaustubhpatange.composecolorplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
