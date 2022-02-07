package no.nav.fo.veilarbregistrering.arbeidsforhold.adapter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.fo.veilarbregistrering.arbeidsforhold.ArbeidsforholdGateway
import no.nav.fo.veilarbregistrering.bruker.Foedselsnummer
import no.nav.fo.veilarbregistrering.config.CacheConfig
import org.junit.jupiter.api.*
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext

class ArbeidsforholdGatewayTest {

    private lateinit var aaregRestClient: AaregRestClient
    private lateinit var context: AnnotationConfigApplicationContext

    @BeforeEach
    fun setup() {
        aaregRestClient = mockk(relaxed = true)
        val beanDefinition: BeanDefinition =
            BeanDefinitionBuilder.rootBeanDefinition(ArbeidsforholdGatewayImpl::class.java)
                .addConstructorArgValue(aaregRestClient).beanDefinition
        context = AnnotationConfigApplicationContext()
        context.register(CacheConfig::class.java, CacheAutoConfiguration::class.java)
        context.defaultListableBeanFactory.registerBeanDefinition("arbeidsforhold", beanDefinition)
        context.refresh()
        context.start()
    }

    @AfterEach
    fun tearDown() {
        context.stop()
    }

    @Test
    fun skalCacheVedKallPaaSammeFnr() {
        val arbeidsforholdGateway = context.getBean(
            ArbeidsforholdGateway::class.java
        )
        every { aaregRestClient.finnArbeidsforhold(any()) } returns emptyList()
        arbeidsforholdGateway.hentArbeidsforhold(IDENT_1)
        arbeidsforholdGateway.hentArbeidsforhold(IDENT_1)
        verify(exactly = 1) { aaregRestClient.finnArbeidsforhold(any()) }
    }

    @Test
    fun skalIkkeCacheVedKallPaaForskjelligFnr() {
        val arbeidsforholdGateway = context.getBean(
            ArbeidsforholdGateway::class.java
        )
        every { aaregRestClient.finnArbeidsforhold(any()) } returns emptyList()
        arbeidsforholdGateway.hentArbeidsforhold(IDENT_1)
        arbeidsforholdGateway.hentArbeidsforhold(IDENT_2)

        verify(exactly = 1) { aaregRestClient.finnArbeidsforhold(IDENT_2) }
    }

    companion object {
        private val IDENT_1 = Foedselsnummer("12345678910")
        private val IDENT_2 = Foedselsnummer("109987654321")
    }
}
