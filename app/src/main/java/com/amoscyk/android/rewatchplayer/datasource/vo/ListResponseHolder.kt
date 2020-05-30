package com.amoscyk.android.rewatchplayer.datasource.vo

class ListResponseHolder<T> private constructor(
    val accumulatedItems: List<T>,
    val newItems: List<T>,
    val isEndOfList: Boolean = false
) {
    constructor(items: List<T> = emptyList()) : this(emptyList(), items)

    fun addNew(newItems: List<T>, isEndOfList: Boolean = false): ListResponseHolder<T> =
        ListResponseHolder(this.accumulatedItems + this.newItems, newItems, isEndOfList)
}
