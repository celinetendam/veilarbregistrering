package no.nav.fo.veilarbregistrering.autorisasjon

import no.nav.common.abac.Pep
import no.nav.common.abac.VeilarbPepFactory
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier
import no.nav.fo.veilarbregistrering.config.requireProperty
import no.nav.fo.veilarbregistrering.Application
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

@Configuration
class PepConfig {

    @Bean
    fun veilarbPep(): Pep {

        val username = getVaultSecret("serviceuser_creds/username")
        val password = getVaultSecret("serviceuser_creds/password")

        val ABAC_URL_PROPERTY = "ABAC_PDP_ENDPOINT_URL"

        return VeilarbPepFactory.get(
                requireProperty(ABAC_URL_PROPERTY),
                username,
                password,
                SpringAuditRequestInfoSupplier())
    }

    private fun getVaultSecret(path: String): String? {
        return try {
            String(Files.readAllBytes(Paths.get(Application.SECRETS_PATH, path)), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalStateException(String.format("Klarte ikke laste property fra vault for path: %s", path), e)
        }
    }
}