package org.github.telegabots.api

data class BlockStateInfo(
    val blockId: Long,
    val states: List<StateItem>
)
