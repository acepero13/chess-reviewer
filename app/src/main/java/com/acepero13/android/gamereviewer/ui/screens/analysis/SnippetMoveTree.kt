package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.chess.core.ui.components.TreeDisplayItem

internal class SnippetMoveTree(private val rootFen: String) {

    data class Node(
        val id: Long,
        val san: String,
        val postFen: String,
        val parentId: Long?,
        val children: MutableList<Long> = mutableListOf(),
        val moveNumber: Int,
        val isWhiteMove: Boolean,
    )

    private val nodes = LinkedHashMap<Long, Node>()
    private var nextId = 1L
    val rootId = 0L
    var currentId = rootId
        private set

    init {
        nodes[rootId] = Node(rootId, "", rootFen, null, moveNumber = 0, isWhiteMove = false)
    }

    fun addMove(san: String, postFen: String): Long {
        val cur = nodes[currentId]!!
        val existing = cur.children.firstOrNull { nodes[it]?.postFen == postFen }
        if (existing != null) {
            currentId = existing
            return existing
        }
        val preFen = cur.postFen
        val isWhite = preFen.split(" ").getOrNull(1) == "w"
        val moveNum = preFen.split(" ").getOrNull(5)?.toIntOrNull() ?: 1
        val id = nextId++
        nodes[id] = Node(id, san, postFen, currentId, moveNumber = moveNum, isWhiteMove = isWhite)
        cur.children.add(id)
        currentId = id
        return id
    }

    fun navigateTo(id: Long): String? {
        val node = nodes[id] ?: return null
        currentId = id
        return node.postFen
    }

    fun stepBack(): String? {
        val cur = nodes[currentId] ?: return null
        val parentId = cur.parentId ?: return null
        currentId = parentId
        return nodes[parentId]!!.postFen
    }

    fun resetToRoot() { currentId = rootId }

    fun canGoBack() = currentId != rootId

    fun buildTreeItems(): List<TreeDisplayItem> {
        if (nodes[rootId]!!.children.isEmpty()) return emptyList()
        return buildMainLine(rootId, needsNumber = true)
    }

    private fun buildMainLine(nodeId: Long, needsNumber: Boolean): List<TreeDisplayItem> {
        val node = nodes[nodeId] ?: return emptyList()
        val items = mutableListOf<TreeDisplayItem>()

        val mainChildId = node.children.firstOrNull() ?: return emptyList()
        val mainChild = nodes[mainChildId]!!

        items.add(
            TreeDisplayItem.MoveItem(
                nodeId         = mainChildId,
                san            = mainChild.san,
                fen            = mainChild.postFen,
                comment        = "",
                hasAnnotations = false,
                isCurrentMove  = currentId == mainChildId,
                depth          = 0,
                moveNumber     = mainChild.moveNumber,
                isWhiteMove    = mainChild.isWhiteMove,
                showMoveNumber = mainChild.isWhiteMove || needsNumber,
            )
        )
        items.addAll(buildMainLine(mainChildId, needsNumber = false))

        for (varChildId in node.children.drop(1)) {
            val varChild = nodes[varChildId] ?: continue
            items.add(TreeDisplayItem.VariationOpen(0))
            items.add(
                TreeDisplayItem.MoveItem(
                    nodeId         = varChildId,
                    san            = varChild.san,
                    fen            = varChild.postFen,
                    comment        = "",
                    hasAnnotations = false,
                    isCurrentMove  = currentId == varChildId,
                    depth          = 0,
                    moveNumber     = varChild.moveNumber,
                    isWhiteMove    = varChild.isWhiteMove,
                    showMoveNumber = true,
                )
            )
            items.addAll(buildMainLine(varChildId, needsNumber = false))
            items.add(TreeDisplayItem.VariationClose(0))
        }

        return items
    }
}
