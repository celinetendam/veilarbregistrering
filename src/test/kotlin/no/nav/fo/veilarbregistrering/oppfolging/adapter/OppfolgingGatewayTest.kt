package no.nav.fo.veilarbregistrering.oppfolging.adapter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.fo.veilarbregistrering.arbeidssoker.Formidlingsgruppe
import no.nav.fo.veilarbregistrering.bruker.AktorId
import no.nav.fo.veilarbregistrering.bruker.Bruker
import no.nav.fo.veilarbregistrering.bruker.Foedselsnummer
import no.nav.fo.veilarbregistrering.config.RequestContext
import no.nav.fo.veilarbregistrering.oppfolging.OppfolgingGateway
import no.nav.fo.veilarbregistrering.oppfolging.Oppfolgingsstatus
import no.nav.fo.veilarbregistrering.registrering.bruker.SykmeldtRegistreringTestdataBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockserver.integration.ClientAndServer
import org.mockserver.junit.jupiter.MockServerExtension
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import javax.servlet.http.HttpServletRequest

@ExtendWith(MockServerExtension::class)
internal class OppfolgingGatewayTest(private val mockServer: ClientAndServer) {

    private lateinit var oppfolgingGateway: OppfolgingGateway
    private lateinit var oppfolgingClient: OppfolgingClient

    @BeforeEach
    fun setup() {
        oppfolgingClient = buildOppfolgingClient()
        oppfolgingGateway = OppfolgingGatewayImpl(oppfolgingClient, mockk(relaxed = true))
    }

    @AfterEach
    fun tearDown() {
        mockServer.reset()
    }

    private fun buildOppfolgingClient(): OppfolgingClient {
        val httpServletRequest: HttpServletRequest = mockk()
        mockkStatic(RequestContext::class)
        every { RequestContext.servletRequest() } returns httpServletRequest
        val baseUrl = "http://" + mockServer.remoteAddress().address.hostName + ":" + mockServer.remoteAddress().port
        return OppfolgingClient(
            jacksonObjectMapper().findAndRegisterModules(),
            mockk(relaxed = true),
            baseUrl,
            mockk(relaxed = true)
        ) { "TOKEN" }.also { oppfolgingClient = it }
    }

    @Test
    fun `Test toString gir mening`() {
        val oppfolgingsstatus = Oppfolgingsstatus(true, false, false, Formidlingsgruppe("ARBS"), null, null)
        val s = oppfolgingsstatus.toString()
        println(s)

    }

    @Test
    fun testAtRegistreringAvSykmeldtGirOk() {
        val sykmeldtRegistrering = SykmeldtRegistreringTestdataBuilder.gyldigSykmeldtRegistrering()
        mockServer.`when`(HttpRequest.request().withMethod("POST").withPath("/oppfolging/aktiverSykmeldt")).respond(
            HttpResponse.response().withStatusCode(204)
        )
        oppfolgingGateway.aktiverSykmeldt(BRUKER.gjeldendeFoedselsnummer, sykmeldtRegistrering.besvarelse)
    }

    @Test
    fun testAtGirInternalServerErrorExceptionDersomRegistreringAvSykmeldtFeiler() {
        val sykmeldtRegistrering = SykmeldtRegistreringTestdataBuilder.gyldigSykmeldtRegistrering()
        mockServer
            .`when`(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath("/oppfolging/aktiverSykmeldt")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(502)
            )
        Assertions.assertThrows(RuntimeException::class.java) {
            oppfolgingGateway.aktiverSykmeldt(
                BRUKER.gjeldendeFoedselsnummer,
                sykmeldtRegistrering.besvarelse
            )
        }
    }

    companion object {
        private const val IDENT = "10108000398" //Aremark fiktivt fnr.";;
        private val BRUKER = Bruker(Foedselsnummer(IDENT), AktorId("AKTØRID"))
    }
}
