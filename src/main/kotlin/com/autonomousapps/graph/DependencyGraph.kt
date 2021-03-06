package com.autonomousapps.graph

internal class DependencyGraph {

  private var edgeCount = 0
  private val adj = LinkedHashMap<String, MutableSet<Edge>>()
  private val nodes = LinkedHashMap<String, Node>()
  private val inDegree = LinkedHashMap<String, Int>()

  companion object {
    fun newGraph(edges: Iterable<Edge>): DependencyGraph {
      val graph = DependencyGraph()
      edges.forEach {
        graph.addEdge(it)
      }
      return graph
    }
  }

  fun addEdge(from: String, to: String) {
    addEdge(Edge(BareNode(from), BareNode(to)))
  }

  fun addEdge(edge: Edge) {
    var added = true

    // Update adjacency lists with new edge
    adj.merge(edge.from.identifier, mutableSetOf(edge)) { set, increment ->
      set.apply { added = addAll(increment) }
    }

    // Only increment these things if we actually added a new edge. This graph does not support
    // parallel edges
    if (added) {
      inDegree.merge(edge.to.identifier, 1) { old, new -> old + new }
      edgeCount++
    }

    // Ensure both nodes are present in the graph. This also has the effect of permitting graphs
    // with only a single node (and no edges).
    addNode(edge.to)
    addNode(edge.from)
  }

  fun addNode(node: String) {
    addNode(BareNode(node))
  }

  fun addNode(node: Node) {
    // Ensure every node, including leaves, is in the adjacency map
    adj.computeIfAbsent(node.identifier) { mutableSetOf() }
    // Updated mapping of node identifier -> node
    nodes.computeIfAbsent(node.identifier) { node }
    // ensure every node has some in-degree value set
    inDegree.computeIfAbsent(node.identifier) { 0 }
  }

  /**
   * The root node. That is, the node with an in-degree of 0. (There should be only one!)
   * TODO now that a graph can have orphaned nodes, this no longer works
   */
  val rootNode: Node by lazy {
    val root = inDegree.entries.find { (_, inDegree) ->
      inDegree == 0
    }?.key ?: error("could not find root node")

    nodes[root] ?: missingNode(root)
  }

  fun nodeCount(): Int = adj.size
  fun edgeCount(): Int = edgeCount

  /**
   * Returns the edges adjacent from node `from` in this digraph.
   *
   * @param  from the node
   * @return the edges adjacent from node `from` in this digraph, as an iterable
   * @throws IllegalArgumentException unless `from` is in the graph.
   */
  fun adj(from: Node): Iterable<Edge> = adj(from.identifier)

  /**
   * Returns the edges adjacent from node `from` in this digraph.
   *
   * @param  from the node
   * @return the edges adjacent from node `from` in this digraph, as an iterable
   * @throws IllegalArgumentException unless `from` is in the graph.
   */
  fun adj(from: String): Iterable<Edge> = adj[from] ?: missingNode(from)

  fun edges(): List<Edge> = nodes.flatMap { (_, node) ->
    adj(node)
  }

  fun nodes(): Iterable<Node> = nodes.map { (_, node) -> node }

  fun hasNode(node: Node): Boolean = hasNode(node.identifier)
  fun hasNode(node: String): Boolean = nodes[node] != null

  /*
   * The functions below all return a new graph.
   */

  fun reversed(): DependencyGraph {
    val reversed = DependencyGraph()
    adj.forEach { (node, edges) ->
      edges.forEach { edge ->
        reversed.addEdge(Edge(edge.to, edge.from, edge.weight))
      }
      reversed.addNode(node)
    }
    return reversed
  }

  fun subgraph(node: String): DependencyGraph {
    return DepthFirstSearch(this, node).subgraph
  }

   fun removeEdge(from: String, to: String): DependencyGraph {
     val graph = DependencyGraph()
     edges().forEach { edge ->
       if (!(edge.from.identifier == from && edge.to.identifier == to)) {
         graph.addEdge(edge)
       }
     }
     // Removing an edge should not be equivalent to removing a node
     graph.addNode(from)
     graph.addNode(to)
     return graph
   }
}

internal fun missingNode(node: Node): Nothing = missingNode(node.identifier)
internal fun missingNode(node: String): Nothing =
  throw IllegalArgumentException("Node $node is not in the graph")

internal operator fun DependencyGraph.plus(other: DependencyGraph): DependencyGraph = apply {
  other.edges().forEach { addEdge(it) }
}

internal fun Iterable<DependencyGraph>.merge(): DependencyGraph = reduce { acc, dependencyGraph ->
  acc + dependencyGraph
}
