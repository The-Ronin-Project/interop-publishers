package com.projectronin.interop.aidbox.testcontainer.container

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.github.dockerjava.api.command.InspectContainerResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.binary.Base64
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName
import java.nio.charset.Charset

/**
 * [Testcontainer](https://www.testcontainers.org/) for [Aidbox](https://www.health-samurai.io/aidbox).
 *
 * This container requires a [databaseContainer], which will be marked as a dependency of this container to ensure
 * proper load order. This container can also be provided with the following options:
 * * [version] - the version of devbox that should be loaded ("latest" by default)
 * * [fhirVersion] - the version of FHIR supported by Aidbox ("4.0.0" by default)
 * * [aidboxClientId] - the client ID registered as an authorized client
 * * [aidboxClientSecret] - the secret registered to the authorized client
 */
class AidboxContainer(
    private val databaseContainer: AidboxDatabaseContainer,
    private val version: String = "2112", // "latest",
    private val fhirVersion: String = "4.0.0",
    val aidboxClientId: String = "test-client",
    val aidboxClientSecret: String = "testclientsecret"
) :
    GenericContainer<AidboxContainer>(DockerImageName.parse("docker-proxy.devops.projectronin.io/healthsamurai/devbox:$version")) {
    init {
        // This has to be specified in the init to ensure that the containers are loaded in the correct order by Testcontainers
        dependsOn(databaseContainer)
    }

    private val internalPort = 9999
    private val rootClientId = "root"
    private val rootClientSecret = "secret"

    /**
     * Ktor Client used by the AidboxContainer. It is also available for use by any consumers of the container.
     */
    val ktorClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            }
        }
    }

    /**
     * The base URL for accessing this AidboxContainer from tests. The URL is guaranteed to not end with a backslash to
     * improve readability for consumers.
     */
    fun baseUrl(): String = "http://localhost:${port()}"

    /**
     * The port at which this Aidbox container can be accessed.
     */
    fun port(): Int = getMappedPort(internalPort)

    /**
     * Retrieves a token capable of accessing any resource on this Aidbox instance.
     */
    fun accessToken(): String {
        val authToken = runBlocking {
            getAuthToken()
        }

        return authToken.accessToken
    }

    override fun configure() {
        withNetwork(Network.SHARED)
        addExposedPort(internalPort)

        withEnv(
            mapOf<String, String>(
                "AIDBOX_LICENSE_ID" to System.getenv("AIDBOX_LICENSE_ID"),
                "AIDBOX_LICENSE_KEY" to System.getenv("AIDBOX_LICENSE_KEY"),
                "AIDBOX_PORT" to internalPort.toString(),
                "AIDBOX_FHIR_VERSION" to fhirVersion,
                "AIDBOX_CLIENT_ID" to rootClientId,
                "AIDBOX_CLIENT_SECRET" to rootClientSecret,
                "PGHOST" to databaseContainer.host,
                "PGPORT" to databaseContainer.port.toString(),
                "PGUSER" to databaseContainer.user,
                "PGPASSWORD" to databaseContainer.password,
                "PGDATABASE" to databaseContainer.schema
            )
        )
    }

    override fun containerIsStarted(containerInfo: InspectContainerResponse?, reused: Boolean) {
        // If it's re-used, then our client and policy should already exist
        if (!reused) {
            containerIsStarted(containerInfo)
        }
    }

    override fun containerIsStarted(containerInfo: InspectContainerResponse?) {
        runBlocking { addClient() }
    }

    /**
     * Builds a Basic auth string based off the [user] and [secret]
     */
    private fun getBasicAuthString(user: String, secret: String) =
        String(Base64.encodeBase64("$user:$secret".toByteArray(Charset.forName("US-ASCII"))))

    /**
     * Adds an authorized client and open access policy to the Aidbox instance.
     */
    private suspend fun addClient() {
        val rootAuthString = getBasicAuthString(rootClientId, rootClientSecret)

        val clientCreateResponse =
            ktorClient.put<HttpResponse>("http://localhost:${port()}/Client/$aidboxClientId") {
                headers {
                    append(HttpHeaders.Authorization, "Basic $rootAuthString")
                }
                accept(ContentType.Application.Json)

                body = TextContent(
                    """
                    |secret: $aidboxClientSecret
                    |grant_types:
                    |  - client_credentials
                    """.trimMargin(),
                    contentType = ContentType("text", "yaml")
                )
            }

        if (!clientCreateResponse.status.isSuccess()) {
            throw IllegalStateException("Error while creating test client for Aidbox: ${clientCreateResponse.receive<String>()}")
        }

        val accessPolicyResponse =
            ktorClient.put<HttpResponse>("http://localhost:${port()}/AccessPolicy/$aidboxClientId") {
                headers {
                    append(HttpHeaders.Authorization, "Basic $rootAuthString")
                }
                accept(ContentType.Application.Json)

                body = TextContent(
                    """
                    |engine: allow
                    |link:
                    |  - id: $aidboxClientId
                    |    resourceType: Client
                    """.trimMargin(),
                    contentType = ContentType("text", "yaml")
                )
            }

        if (!accessPolicyResponse.status.isSuccess()) {
            throw IllegalStateException("Error while creating access policy for Aidbox test client: ${clientCreateResponse.receive<String>()}")
        }
    }

    /**
     * Gets an auth token for the authorized client configured for this container.
     */
    private suspend fun getAuthToken(): AuthToken {
        val tokenResponse =
            ktorClient.post<HttpResponse>("http://localhost:${port()}/auth/token") {
                headers {
                    append(HttpHeaders.Authorization, "Basic ${getBasicAuthString(aidboxClientId, aidboxClientSecret)}")
                }
                contentType(ContentType.Application.FormUrlEncoded)
                accept(ContentType.Application.Json)

                body = "grant_type=client_credentials"
            }

        if (!tokenResponse.status.isSuccess()) {
            throw IllegalStateException("Error retrieving auth token: ${tokenResponse.receive<String>()}")
        }

        return tokenResponse.receive()
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class AuthToken(val accessToken: String)
}
