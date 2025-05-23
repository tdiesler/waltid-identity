@file:OptIn(ExperimentalUuidApi::class)

package id.walt.webwallet.usecase.credential

import id.walt.webwallet.service.credentials.CredentialStatusServiceFactory
import id.walt.webwallet.service.credentials.CredentialsService
import id.walt.webwallet.service.credentials.status.StatusListEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


class CredentialStatusUseCase(
    private val credentialService: CredentialsService,
    private val credentialStatusServiceFactory: CredentialStatusServiceFactory,
) {
    suspend fun get(wallet: Uuid, credentialId: String): List<CredentialStatusResult> =
        credentialService.get(wallet, credentialId)?.parsedDocument?.let {
            getStatusEntry(it).fold(emptyList()) { acc, i ->
                acc.plus(credentialStatusServiceFactory.new(i.type).get(i))
            }
        } ?: error("Credential not found or invalid document for id: $credentialId")

    private val jsonModule = Json { ignoreUnknownKeys = true }

    private fun getStatusEntry(json: JsonObject) = json.jsonObject["credentialStatus"]?.let {
        when (it) {
            is JsonArray -> jsonModule.decodeFromJsonElement<List<StatusListEntry>>(it)
            is JsonObject -> listOf(jsonModule.decodeFromJsonElement<StatusListEntry>(it))
            else -> null
        }
    } ?: emptyList()
}

@Serializable
data class CredentialStatusResult(
    val type: String,
    val result: Boolean,
    val message: String,
)
