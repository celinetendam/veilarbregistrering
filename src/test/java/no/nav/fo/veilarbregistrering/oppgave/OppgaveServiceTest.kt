package no.nav.fo.veilarbregistrering.oppgave

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.fo.veilarbregistrering.bruker.AktorId
import no.nav.fo.veilarbregistrering.bruker.Bruker
import no.nav.fo.veilarbregistrering.bruker.Foedselsnummer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OppgaveServiceTest {
    private lateinit var oppgaveService: OppgaveService
    private lateinit var oppgaveGateway: OppgaveGateway
    private lateinit var oppgaveRepository: OppgaveRepository
    private lateinit var oppgaveRouter: OppgaveRouter

    @BeforeEach
    fun setUp() {
        oppgaveGateway = mockk()
        oppgaveRepository = mockk(relaxed = true)
        oppgaveRouter = mockk()
        oppgaveService = CustomOppgaveService(oppgaveGateway, oppgaveRepository, oppgaveRouter)
    }

    @Test
    fun `opprettOppgave ang opphold skal gi beskrivelse om rutine`() {
        every { oppgaveRouter.hentEnhetsnummerFor(BRUKER) } returns Optional.empty()
        every { oppgaveGateway.opprett(any()) } returns DummyOppgaveResponse()
        oppgaveService.opprettOppgave(BRUKER, OppgaveType.OPPHOLDSTILLATELSE)
        val oppgave = Oppgave.opprettOppgave(
            BRUKER.aktorId,
            null, OppgaveType.OPPHOLDSTILLATELSE,
            LocalDate.of(2020, 4, 10)
        )
        verify(exactly = 1) { oppgaveGateway.opprett(oppgave) }
    }

    @Test
    fun `opprettOppgave ang dod utvandret skal gi beskrivelse om rutine`() {
        every { oppgaveRouter.hentEnhetsnummerFor(BRUKER) } returns Optional.empty()
        every { oppgaveGateway.opprett(any()) } returns DummyOppgaveResponse()
        oppgaveService.opprettOppgave(BRUKER, OppgaveType.UTVANDRET)
        val oppgave = Oppgave.opprettOppgave(
            BRUKER.aktorId,
            null, OppgaveType.UTVANDRET,
            LocalDate.of(2020, 4, 10)
        )
        verify(exactly = 1) { oppgaveGateway.opprett(oppgave) }
    }

    @Test
    fun `skal lagre oppgave ved vellykket opprettelse av oppgave`() {
        every { oppgaveRouter.hentEnhetsnummerFor(BRUKER) } returns Optional.empty()
        every { oppgaveGateway.opprett(any()) } returns DummyOppgaveResponse()
        oppgaveService.opprettOppgave(BRUKER, OppgaveType.OPPHOLDSTILLATELSE)
        verify(exactly = 1) { oppgaveRepository.opprettOppgave(BRUKER.aktorId, OppgaveType.OPPHOLDSTILLATELSE, 234L) }
    }

    @Test
    fun `skal kaste exception dersom det finnes nyere oppholdsoppgave fra for`() {
        val oppgaveSomBleOpprettetDagenFor =
            OppgaveImpl(23, BRUKER.aktorId, OppgaveType.OPPHOLDSTILLATELSE, 23, LocalDateTime.of(2020, 4, 9, 22, 0))
        val oppgaver = listOf(oppgaveSomBleOpprettetDagenFor)
        every { oppgaveRepository.hentOppgaverFor(any()) } returns oppgaver
        Assertions.assertThrows(OppgaveAlleredeOpprettet::class.java) {
            oppgaveService.opprettOppgave(
                BRUKER,
                OppgaveType.OPPHOLDSTILLATELSE
            )
        }
        verify { oppgaveGateway wasNot Called }
    }

    @Test
    fun `skal kaste exception dersom det finnes nyere utvandretoppgave fra for`() {
        val oppgaveSomBleOpprettetDagenFor =
            OppgaveImpl(23, BRUKER.aktorId, OppgaveType.UTVANDRET, 23, LocalDateTime.of(2020, 4, 9, 22, 0))
        val oppgaver = listOf(oppgaveSomBleOpprettetDagenFor)
        every { oppgaveRepository.hentOppgaverFor(any()) } returns oppgaver
        Assertions.assertThrows(OppgaveAlleredeOpprettet::class.java) { oppgaveService.opprettOppgave(BRUKER, OppgaveType.UTVANDRET) }
        verify { oppgaveGateway wasNot Called }
    }

    @Test
    fun `skal ikke kaste exception dersom det finnes eldre oppgave fra for`() {
        every { oppgaveRouter.hentEnhetsnummerFor(BRUKER) } returns Optional.empty()
        val oppgaveSomBleOpprettetTreDagerFor =
            OppgaveImpl(23, BRUKER.aktorId, OppgaveType.OPPHOLDSTILLATELSE, 23, LocalDateTime.of(2020, 3, 10, 22, 0))
        val oppgaver = listOf(oppgaveSomBleOpprettetTreDagerFor)
        every { oppgaveRepository.hentOppgaverFor(any()) } returns oppgaver
        every { oppgaveGateway.opprett(any()) } returns DummyOppgaveResponse()
        oppgaveService.opprettOppgave(BRUKER, OppgaveType.OPPHOLDSTILLATELSE)
        val oppgave = Oppgave.opprettOppgave(
            BRUKER.aktorId,
            null, OppgaveType.OPPHOLDSTILLATELSE,
            LocalDate.of(2020, 4, 10)
        )
        verify(exactly = 1) { oppgaveGateway.opprett(oppgave) }
    }

    @Test
    fun `ingen tidligere oppgaver`() {
        every { oppgaveRouter.hentEnhetsnummerFor(BRUKER) } returns Optional.empty()
        every { oppgaveRepository.hentOppgaverFor(any()) } returns emptyList()
        every { oppgaveGateway.opprett(any()) } returns DummyOppgaveResponse()
        oppgaveService.opprettOppgave(BRUKER, OppgaveType.OPPHOLDSTILLATELSE)
        val oppgave = Oppgave.opprettOppgave(
            BRUKER.aktorId,
            null, OppgaveType.OPPHOLDSTILLATELSE,
            LocalDate.of(2020, 4, 10)
        )
        verify(exactly = 1) { oppgaveGateway.opprett(oppgave) }
    }

    @Test
    fun `skal ikke kaste exception dersom det finnes oppgave av annen type`() {
        every { oppgaveRouter.hentEnhetsnummerFor(BRUKER) } returns Optional.empty()
        val oppgaveSomBleOpprettetEnDagerFor =
            OppgaveImpl(23, BRUKER.aktorId, OppgaveType.OPPHOLDSTILLATELSE, 23, LocalDateTime.of(2020, 4, 9, 22, 0))
        val oppgaver = listOf(oppgaveSomBleOpprettetEnDagerFor)
        every { oppgaveRepository.hentOppgaverFor(any()) } returns oppgaver
        every { oppgaveGateway.opprett(any()) } returns DummyOppgaveResponse()
        oppgaveService.opprettOppgave(BRUKER, OppgaveType.UTVANDRET)
        val oppgave = Oppgave.opprettOppgave(
            BRUKER.aktorId,
            null, OppgaveType.UTVANDRET,
            LocalDate.of(2020, 4, 10)
        )
        verify(exactly = 1) { oppgaveGateway.opprett(oppgave) }
    }

    private class DummyOppgaveResponse : OppgaveResponse {
        override val id: Long = 234L
        override val tildeltEnhetsnr: String = "0393"
    }

    private class CustomOppgaveService(
        oppgaveGateway: OppgaveGateway,
        oppgaveRepository: OppgaveRepository,
        oppgaveRouter: OppgaveRouter,
    ) : OppgaveService(
        oppgaveGateway,
        oppgaveRepository,
        oppgaveRouter,
        mockk(relaxed = true)
    ) {
        override fun idag(): LocalDate {
            return LocalDate.of(2020, 4, 10)
        }
    }

    companion object {
        private val BRUKER = Bruker(Foedselsnummer("12345678911"), AktorId("2134"))
    }
}