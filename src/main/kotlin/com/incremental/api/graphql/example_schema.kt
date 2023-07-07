package com.incremental.api.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.toSchema
import graphql.ExecutionInput.Builder
import graphql.GraphQL.newGraphQL
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import org.http4k.graphql.GraphQLHandler
import org.http4k.graphql.GraphQLRequest
import org.http4k.graphql.GraphQLResponse
import java.util.concurrent.CompletableFuture.supplyAsync

object UserDb {
    private val userDb = mutableListOf(
        User(id = 1, name = "Jim"),
        User(id = 2, name = "Bob"),
        User(id = 3, name = "Sue"),
        User(id = 4, name = "Rita"),
        User(id = 5, name = "Charlie")
    )

    fun search(ids: List<Int>) = userDb.filter { ids.contains(it.id) }
    fun delete(ids: List<Int>) = userDb.removeIf { ids.contains(it.id) }
}

data class User(val id: Int, val name: String)

class UserQueries {
    fun search(params: Params) = UserDb.search(params.ids)
}

class UserMutations {
    fun delete(params: Params) = UserDb.delete(params.ids)
}

data class Params(val ids: List<Int>)

class UserDbHandler : GraphQLHandler {
    private val graphQL = newGraphQL(
        toSchema(
            SchemaGeneratorConfig(supportedPackages = listOf("com.incremental.api.graphql")),
            listOf(TopLevelObject(UserQueries())),
            listOf(TopLevelObject(UserMutations()))
        )
    ).build()

    private val dataLoaderRegistry = DataLoaderRegistry().apply {
        register("USER_LOADER", DataLoader { ids: List<Int> ->
            supplyAsync {
                UserQueries().search(Params(ids))
            }
        })
    }

    override fun invoke(payload: GraphQLRequest) = GraphQLResponse.from(
        graphQL.execute(
            Builder()
                .query(payload.query)
                .variables(payload.variables)
                .dataLoaderRegistry(dataLoaderRegistry)
        )
    )
}
