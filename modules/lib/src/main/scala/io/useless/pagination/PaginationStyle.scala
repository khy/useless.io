package io.useless.pagination

/*
 * PagintionStyle describes how the client specifies how many resources to
 * skip before the result set. It is orthoganal to order and limit.
 */
sealed trait PaginationStyle

/*
 * OffsetBasedPagination allows the client to skip a number of resources before
 * the result set.
 */
case object OffsetBasedPagination extends PaginationStyle

/*
 * PageBasedPagination allows the client to skip a number of pages before the
 * result set, which basically reduces to OffsetBasedPagination: 'page' amounts
 * to an offset of (page - 1) * limit.
 */
case object PageBasedPagination extends PaginationStyle

/*
 * PrecedenceBasedPagination allows the client to choose which resource the
 * result set should start after.
 */
case object PrecedenceBasedPagination extends PaginationStyle
