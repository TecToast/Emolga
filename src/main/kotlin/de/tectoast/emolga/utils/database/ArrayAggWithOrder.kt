package de.tectoast.emolga.utils.database

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.Function

class ArrayAggWithOrder<T : Any>(
    private val expr: Expression<T>,
    private val orderBy: Expression<*>,
    private val sortOrder: SortOrder,
    columnType: IColumnType<T>
) : Function<List<T>>(ArrayColumnType<T, List<T>>(columnType)) {

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("array_agg(")
        queryBuilder.append(expr)
        queryBuilder.append(" ORDER BY ")
        queryBuilder.append(orderBy)
        queryBuilder.append(" ")
        queryBuilder.append(sortOrder.code)
        queryBuilder.append(")")
    }
}

fun <T : Any> Expression<T>.arrayAgg(
    orderBy: Expression<*>,
    sortOrder: SortOrder = SortOrder.ASC,
    columnType: IColumnType<T>
) = ArrayAggWithOrder(this, orderBy, sortOrder, columnType)