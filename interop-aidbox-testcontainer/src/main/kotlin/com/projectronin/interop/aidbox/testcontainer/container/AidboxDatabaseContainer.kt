package com.projectronin.interop.aidbox.testcontainer.container

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName

/**
 * [Testcontainer](https://www.testcontainers.org/) for [Aidbox](https://www.health-samurai.io/aidbox)
 */
class AidboxDatabaseContainer(version: String = "13.2", val port: Int = 5432) :
    GenericContainer<AidboxContainer>(DockerImageName.parse("healthsamurai/aidboxdb:$version")) {
    /**
     * The hostname within Docker.
     */
    internal val host = "database"

    /**
     * The username for accessing the database instance.
     */
    internal val user = "postgres"

    /**
     * The password for accessing the database instance.
     */
    internal val password = "postgres"

    /**
     * The specific schema created on this database instance.
     */
    internal val schema = "devbox"

    internal val network = Network.newNetwork()

    override fun configure() {
        withNetwork(network)
        withNetworkAliases(host)

        addExposedPorts(port)

        withEnv(
            mapOf(
                "POSTGRES_USER" to user,
                "POSTGRES_PASSWORD" to password,
                "POSTGRES_DB" to schema
            )
        )
    }
}
