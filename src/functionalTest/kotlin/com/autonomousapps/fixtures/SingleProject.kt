package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.Dependency
import java.io.File

class SingleProject : ProjectDirProvider {

  private val rootSpec = RootSpec(buildScript = buildScript())

  private val rootProject = RootProject(rootSpec)

  override val projectDir: File = rootProject.projectDir

  override fun project(moduleName: String): Module {
    if (moduleName == ":") {
      return rootProject
    } else {
      error("No '$moduleName' project found!")
    }
  }

  companion object {
    private fun buildScript(): String {
      return """
        plugins {
          id 'java-library'
          id 'com.autonomousapps.dependency-analysis' version '${System.getProperty("com.autonomousapps.pluginversion")}'
        }
        
        java {
          sourceCompatibility = JavaVersion.VERSION_1_8
          targetCompatibility = JavaVersion.VERSION_1_8
        }
        
        repositories {
          mavenCentral()
        }
        
        dependencies {
          api 'org.apache.commons:commons-math3:3.6.1'
          implementation 'com.google.guava:guava:28.2-jre'
        }
    """
    }

    @JvmStatic
    fun expectedAdvice() = setOf(
      Advice.ofRemove(Dependency("com.google.guava:guava", "28.2-jre", "implementation")),
      Advice.ofRemove(Dependency("org.apache.commons:commons-math3", "3.6.1", "api"))
    )
  }
}
