package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.graph.*
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

/**
 * This task generates a complete dependencies graph for every subproject in the build, as well as a
 * reverse-dependencies graph which shows which projects might be impacted by a change in another
 * project.
 */
@CacheableTask
abstract class DependencyGraphAllProjects : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a graph of all inter-project dependencies"
  }

  private var query: String = ""

  @Option(option = "id", description = "The project dependency for which to generate a reverse graph")
  fun query(identifier: String) {
    this.query = identifier
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var graphs: Configuration

  @get:OutputFile
  abstract val outputFullGraphJson: RegularFileProperty

  @get:OutputFile
  abstract val outputFullGraphDot: RegularFileProperty

  @get:OutputFile
  abstract val outputRevGraphDot: RegularFileProperty

  @get:OutputFile
  abstract val outputRevSubGraphDot: RegularFileProperty

  @TaskAction fun action() {
    val outputFullGraphJsonFile = outputFullGraphJson.getAndDelete()
    val outputFile = outputFullGraphDot.getAndDelete()
    val outputRevFile = outputRevGraphDot.getAndDelete()
    val outputRevSubFile = outputRevSubGraphDot.getAndDelete()

    val graph = graphs.dependencies
      .filterIsInstance<ProjectDependency>()
      .flatMap { dep ->
        graphs.fileCollection(dep)
          .filter { it.exists() }
          .map { it.fromJson<DependencyGraph>() }
      }.merge()

    val reversed = graph.reversed()

    outputFullGraphJsonFile.writeText(graph.toJson())

    // TODO need to run graphviz automatically
    logger.debug("Graph DOT at ${outputFile.path}")
    outputFile.writeText(GraphWriter.toDot(graph))

    logger.debug("Graph rev DOT at ${outputRevFile.path}")
    outputRevFile.writeText(GraphWriter.toDot(reversed))

    if (query.isNotEmpty()) {
      val node = getQueryNode()
      val subgraph = DepthFirstSearch(reversed, node).subgraph

      logger.quiet("Subgraph rooted on $query at ${outputRevSubFile.path}")
      outputRevSubFile.writeText(GraphWriter.toDot(subgraph))
    }
  }

  private fun getQueryNode(): Node {
    if (!query.startsWith(":")) {
      throw GradleException("You cannot query for a non-project dependency.")
    }

    return ProducerNode(query)
  }
}