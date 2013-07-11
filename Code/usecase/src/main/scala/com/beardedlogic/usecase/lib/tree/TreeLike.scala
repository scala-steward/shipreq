package com.beardedlogic.usecase.lib.tree

/**
 * Anything that has (recursive) child nodes can be considered tree-like.
 *
 * @since 11/07/2013
 */
trait TreeLike[N <: TreeNodeLike[N]] {

  val children: List[N]

  def apply(childIndex: Int) = if (childIndex >= 0) children(childIndex) else children(children.size + childIndex)

  def foreachRecursive(fn: N => Any): Unit = children.foreach(_.foreachRecursive(fn))

  def mapRecursive[R](fn: N => R): List[R] = children.flatMap(_.mapRecursive(fn))
}

object TreeLike {
  def apply[N <: TreeNodeLike[N]](nodes: List[N]): TreeLike[N] = GenTreeLike(nodes)
}

/** A generic implementation for turning a List into a TreeLike. */
case class GenTreeLike[N <: TreeNodeLike[N]](override val children: List[N]) extends TreeLike[N]

/**
 * The root of a tree.
 * Not a node itself, but TreeLike and contains top-level nodes.
 *
 * @since 11/07/2013
 */
trait TreeRoot[N <: TreeNodeLike[N]] extends TreeLike[N] {
  final def size = children.size
  final def isEmpty = children.isEmpty
  final def nonEmpty = children.nonEmpty
  final def head = children.head
  final def headOption = children.headOption
  final def tail = children.tail
  final def tailAsTreeLike: TreeLike[N] = TreeLike(tail)
}

/**
 * Anything that has a list of children, and is itself a value can be considered treenode-like.
 *
 * @since 1/06/2013
 */
trait TreeNodeLike[N <: TreeNodeLike[N]] extends TreeLike[N] {
  self: N =>

  override def foreachRecursive(fn: N => Any) {
    fn(this)
    super.foreachRecursive(fn)
  }

  override def mapRecursive[R](fn: N => R): List[R] = fn(this) +: super.mapRecursive(fn)

  def deepCopy(fn: (N, List[N]) => N): N = {
    val copiedChildren = deepCopyChildren(fn)
    fn(this, copiedChildren)
  }

  def deepCopyChildren(fn: (N, List[N]) => N): List[N] = children.map(_ deepCopy fn)
}