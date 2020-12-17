package no.nav.fo.veilarbregistrering.registrering.bruker

abstract class AktiverBrukerResultat internal constructor() {
    abstract fun erFeil(): Boolean
    abstract fun feil(): AktiverBrukerFeil

    companion object {
        fun ok() = object : AktiverBrukerResultat() {
            override fun erFeil(): Boolean = false

            override fun feil(): AktiverBrukerFeil {
                throw IllegalStateException("Aktivering gikk Ok - ingen feil finnes")
            }
        }

        fun feilFrom(aktiverBrukerFeil: AktiverBrukerFeil): AktiverBrukerResultat {
            return object : AktiverBrukerResultat() {
                override fun erFeil(): Boolean = true

                override fun feil(): AktiverBrukerFeil = aktiverBrukerFeil
            }
        }
    }
}