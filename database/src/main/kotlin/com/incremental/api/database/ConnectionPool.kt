package com.incremental.api.database

interface TransactionManager<Repository> {
    fun <T> tx(block: Repository.() -> T): T
}
