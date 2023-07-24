package accessibility.reporting.tool.wcag

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*


class ReportV1(
    reportId: String,
    url: String,
    organizationUnit: OrganizationUnit,
    testUrl: String? = null,
    successCriteria: List<SuccessCriterion>,
    testpersonIdent: String? = null
) : Report(reportId = reportId, url, organizationUnit, Version.ONE, testUrl, successCriteria, testpersonIdent) {

    companion object {
        fun deserialize(s: String): Report = objectMapper.readValue(s, ReportV1::class.java)

        private val objectMapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
        }

        fun createEmpty(
            url: String,
            organizationUnit: OrganizationUnit,
            testUrl: String?,
            testpersonIdent: String?,
            testId: String? = null
        ) =
            ReportV1(
                // TODO: NO, it's not foo!
                reportId = testId ?: "foo",
                url = url,
                organizationUnit = organizationUnit,
                testUrl = url,
                testpersonIdent = testpersonIdent,
                successCriteria = successCriteriaV1
            )

        val textAlternatives = Guideline(name = "Text Alternatives", section = 1, principle = Principle.PERCEIVABLE)
        val timebasedMedia = Guideline(name = "Timebased media", section = 2, principle = Principle.PERCEIVABLE)
        val adaptable = Guideline(name = "Adatptable", section = 3, principle = Principle.PERCEIVABLE)
        val distinguishable = Guideline(name = "Distinguishable", section = 3, principle = Principle.PERCEIVABLE)
        val successCriteriaV1 = criteria
    }

    override fun toJson() = objectMapper.writeValueAsString(this)
}

val criteria = listOf(
    SuccessCriterion.createEmpty(
        number = "1.1.1",
        name = "1.1.1 Ikke-tekstlig innhold",
        description = "Ikke bruk presentasjon som bygger utelukkende på farge.",
        principle = "1.  Mulig å oppfatte",
        guildeline_ = "1.1 Tekstalternativer",
        contentGroup = "Ikoner, bilder, grafer",
        tools = "ARC Toolkit",
    ),
    SuccessCriterion.createEmpty(
        number = "1.2.1",
        name = "1.2.1 Bare lyd og bare video (forhåndsinnspilt)",
        description =
        "Ikke-tekstlig innhold skal ha et kontrastforhold på minst 3=1 mot farge(r) som ligger ved siden av.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.2 Tidsbaserte medier",
        contentGroup = "Lyd, video, animasjoner",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "1.2.2",
        name = "1.2.2 Teksting (forhåndsinnspilt)",
        description =
        "Kontrastforholdet mellom teksten og bakgrunnen er minst 4,5=1.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.2 Tidsbaserte medier",
        contentGroup = "Lyd, video, animasjoner",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "1.2.3",
        name = "1.2.3 Synstolking eller mediealternativ (forhåndsinnspilt)",
        description = "Innhold skal ikke blinke mer enn tre ganger per sekund.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.2 Tidsbaserte medier",
        contentGroup = "Lyd, video, animasjoner",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "1.2.5",
        name = "1.2.5 Synstolking (forhåndsinnspilt)",
        description =
        "Instruksjoner må ikke utelukkende være avhengige av form, størrelse, visuell plassering, orientering, eller lyd for å kunne bli forstått.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.2 Tidsbaserte medier",
        contentGroup = "Lyd, video, animasjoner",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "1.3.1",
        name = "1.3.1 Informasjon og relasjoner",
        description = "Bruk tekst i stedet for bilder av tekst.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.3 Mulig å tilpasse",
        contentGroup = "",
        tools = "DevTools/headingsMap",
    ),
    SuccessCriterion.createEmpty(
        number = "1.3.2",
        name = "1.3.2 Meningsbærende rekkefølge",
        description = "Bruk nyttige og tydelige sidetitler.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.3 Mulig å tilpasse",
        contentGroup = "",
        tools = "disableHTML",
    ),
    SuccessCriterion.createEmpty(
        number = "1.3.3",
        name = "1.3.3 Sensoriske egenskaper",
        description =
        "Alle lenkers mål og funksjon fremgår tydelig av lenketeksten.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.3 Mulig å tilpasse",
        contentGroup = "",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "1.3.4",
        name = "1.3.4 Visningsretning",
        description = "Tilby brukeren flere måter å navigere på.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.3 Mulig å tilpasse",
        contentGroup = "",
        tools = "DevTools",
    ),
    SuccessCriterion.createEmpty(
        number = "1.3.5",
        name = "1.3.5 Identifiser formål med inndata",
        description = "Sørg for at ledetekster og overskrifter er beskrivende.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.3 Mulig å tilpasse",
        contentGroup = "Skjemaer",
        tools = "DevTools",
    ),
    SuccessCriterion.createEmpty(
        number = "1.4.1",
        name = "1.4.1 Bruk av farge",
        description =
        "Sørg for at språket til innholdet på alle nettsider er angitt i koden.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.4 Mulig å skille fra hverandre",
        contentGroup = "",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "1.4.1",
        name = "1.4.10 Dynamisk tilpasning (Reflow)",
        description =
        "Sørg for at alle deler av innholdet som er på et annet språk enn resten av siden er markert i koden.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.4 Mulig å skille fra hverandre",
        contentGroup = "",
        tools = "Zoom + DevTools",
    ),
    SuccessCriterion.createEmpty(
        number = "1.4.1",
        name = "1.4.11 Kontrast for ikke-tekstlig innhold",
        description =
        "Navigasjonslinker som gjentas på flere sider skal ha en konsekvent rekkefølge.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.4 Mulig å skille fra hverandre",
        contentGroup = "",
        tools = "CCA",
    ),
    SuccessCriterion.createEmpty(
        number = "1.4.1",
        name = "1.4.12 Tekstavstand",
        description =
        "Elementer som har samme funksjonalitet på tvers av flere sider er utformet likt.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.4 Mulig å skille fra hverandre",
        contentGroup = "",
        tools = "ARC Toolkit",
    ),
    SuccessCriterion.createEmpty(
        number = "1.4.1",
        name = "1.4.13 Pekerfølsomt innhold eller innhold ved tastaturfokus",
        description =
        "For feil som oppdages automatisk må du vise hvor feilen har oppstått og gi en tekstbeskrivelse av feilen.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.4 Mulig å skille fra hverandre",
        contentGroup = "",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "1.4.2",
        name = "1.4.2 Styring av lyd",
        description =
        "Det vises ledetekster eller instruksjoner når du har skjemaelementer som må fylles ut.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.4 Mulig å skille fra hverandre",
        contentGroup = "Lyd, video, animasjoner",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "1.4.3",
        name = "1.4.3 Kontrast (minimum)",
        description =
        "Dersom feil blir oppdaget automatisk, gi brukeren et forslag til hvordan feilen kan rettes.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.4 Mulig å skille fra hverandre",
        contentGroup = "",
        tools = "ARC Toolkit/CCA",
    ),
    SuccessCriterion.createEmpty(
        number = "1.4.4",
        name = "1.4.4 Endring av tekststørrelse",
        description =
        "For sider som medfører juridiske forpliktelser må det være mulig å kunne angre, kontrollere eller bekrefte dataene som sendes inn.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.4 Mulig å skille fra hverandre",
        contentGroup = "",
        tools = "Zoom",
    ),
    SuccessCriterion.createEmpty(
        number = "1.4.5",
        name = "1.4.5 Bilder av tekst",
        description = "Innhold på nettsiden skal kunne brukes med enkel pekerinput.",
        principle = "1. Mulig å oppfatte",
        guildeline_ = "1.4 Mulig å skille fra hverandre",
        contentGroup = "Ikoner, bilder, grafer",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "2.1.1",
        name = "2.1.1 Tastatur",
        description =
        "Uheldige og feilaktige input via mus eller berøringsskjerm skal lettere kunne forhindres.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.1 Tilgjengelig med tastatur",
        contentGroup = "",
        tools = "Tastatur",
    ),
    SuccessCriterion.createEmpty(
        number = "2.1.2",
        name = "2.1.2 Ingen tastaturfelle",
        description =
        "Funksjonalitet som kan betjenes med å bevege enheten eller ved brukerbevegelse, skal også kunne betjenes med brukersnittkomponenter.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.1 Tilgjengelig med tastatur",
        contentGroup = "",
        tools = "Tastatur",
    ),
    SuccessCriterion.createEmpty(
        number = "2.1.4",
        name = "2.1.4 Hurtigtaster som består av ett tegn",
        description =
        "Brukeren må få velge om innholdet skal vises i liggende eller stående retning.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.1 Tilgjengelig med tastatur",
        contentGroup = "",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "2.2.1",
        name = "2.2.1 Justerbar hastighet",
        description =
        "Innhold skal kunne endres til 400 prosent størrelse ved 1280 piksler bredde, uten tap av informasjon eller funksjonalitet.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.2 Nok tid",
        contentGroup = "Tidsbegrenset innhold",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "2.2.2",
        name = "2.2.2 Pause, stopp, skjul",
        description =
        "Tekstavstanden skal kunne overstyres for å gjøre teksten lettere å lese.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.2 Nok tid",
        contentGroup = "Innhold som automatisk endrer seg eller oppdaterer",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "2.3.1",
        name = "2.3.1 Terskelverdi på maksimalt tre glimt",
        description =
        "Gi brukeren mulighet til å stoppe eller pause lyd som starter automatisk.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.3 Anfall og andre fysiske reaksjoner",
        contentGroup = "Innhold som blinker",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "2.4.1",
        name = "2.4.1 Hoppe over blokker",
        description =
        "Tekst kan bli endret til 200% størrelse uten tap av innhold eller funksjon.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.4 Navigerbar",
        contentGroup = "",
        tools = "Tastatur",
    ),
    SuccessCriterion.createEmpty(
        number = "2.4.2",
        name = "2.4.2 Sidetitler",
        description =
        "Brukeren skal enkelt kunne slå av hurtigtaster som består av ett tegn.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.4 Navigerbar",
        contentGroup = "",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "2.4.3",
        name = "2.4.3 Fokusrekkefølge",
        description = "Tidsbegrensninger skal kunne justeres av brukeren.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.4 Navigerbar",
        contentGroup = "",
        tools = "Tastatur",
    ),
    SuccessCriterion.createEmpty(
        number = "2.4.4",
        name = "2.4.4 Formål med lenke (i kontekst)",
        description =
        "Gi brukeren mulighet til å stoppe, pause eller skjule innhold som automatisk endrer seg.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.4 Navigerbar",
        contentGroup = "",
        tools = "DevTools",
    ),
    SuccessCriterion.createEmpty(
        number = "2.4.5",
        name = "2.4.5 Flere måter",
        description = "Skjemaelementer er kodet med inndataformål.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.4 Navigerbar",
        contentGroup = "",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "2.4.6",
        name = "2.4.6 Overskrifter og ledetekster",
        description =
        "Brukere som bruker visuelle ledetekster skal også kunne bruke kodede ledetekster.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.4 Navigerbar",
        contentGroup = "",
        tools = "Skjønn/headingsMap",
    ),
    SuccessCriterion.createEmpty(
        number = "2.4.7",
        name = "2.4.7 Synlig fokus",
        description = "Alle sider skal være uten store kodefeil.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.4 Navigerbar",
        contentGroup = "",
        tools = "Tastatur",
    ),
    SuccessCriterion.createEmpty(
        number = "2.5.1",
        name = "2.5.1 Pekerbevegelser",
        description = "Alle komponenter har navn og rolle bestemt i koden.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.5 Inndatametode",
        contentGroup = "",
        tools = "Mus",
    ),
    SuccessCriterion.createEmpty(
        number = "2.5.2",
        name = "2.5.2 Pekeravbrytelse",
        description = "Ting skal være kodet som det det ser ut som.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.5 Inndatametode",
        contentGroup = "",
        tools = "Mus",
    ),
    SuccessCriterion.createEmpty(
        number = "2.5.3",
        name = "2.5.3 Ledetekst i navn",
        description = "Presenter innhold i en meningsfull rekkefølge.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.5 Inndatametode",
        contentGroup = "Skjemaer",
        tools = "DevTools",
    ),
    SuccessCriterion.createEmpty(
        number = "2.5.4",
        name = "2.5.4 Bevegelsesaktivering",
        description =
        "Brukeren skal få statusbeskjeder om viktige endringer på nettsiden uten at det gir kontekstendring.",
        principle = "2. Mulig å betjene",
        guildeline_ = "2.5 Inndaztametode",
        contentGroup = "",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "3.1.1",
        name = "3.1.1 Språk på siden",
        description =
        "Brukeren skal ha mer kontroll over innholdet på nettsiden som får fokus med musepeker eller tastatur.",
        principle = "3. Forståelig",
        guildeline_ = "3.1 Leselig",
        contentGroup = "",
        tools = "DevTools",
    ),
    SuccessCriterion.createEmpty(
        number = "3.1.2",
        name = "3.1.2 Språk på deler av innhold",
        description =
        "All funksjonalitet skal kunne brukes kun ved hjelp av tastatur.",
        principle = "3. Forståelig",
        guildeline_ = "3.1 Leselig",
        contentGroup = "",
        tools = "DevTools",
    ),
    SuccessCriterion.createEmpty(
        number = "3.2.1",
        name = "3.2.1 Fokus",
        description = "Unngå tastaturfeller.",
        principle = "3. Forståelig",
        guildeline_ = "3.2 Forutsigbar",
        contentGroup = "",
        tools = "Tastatur",
    ),
    SuccessCriterion.createEmpty(
        number = "3.2.2",
        name = "3.2.2 Inndata",
        description = "Gi brukeren mulighet til å hoppe direkte til hovedinnholdet.",
        principle = "3. Forståelig",
        guildeline_ = "3.2 Forutsigbar",
        contentGroup = "Skjemaer",
        tools = "Tastatur",
    ),
    SuccessCriterion.createEmpty(
        number = "3.2.3",
        name = "3.2.3 Konsekvent navigering",
        description = "Presenter innholdet i en logisk rekkefølge.",
        principle = "3. Forståelig",
        guildeline_ = "3.2 Forutsigbar",
        contentGroup = "",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "3.2.4",
        name = "3.2.4 Konsekvent identifikasjon",
        description =
        "Sørg for at alt innhold får synlig fokus når du navigerer med tastatur.",
        principle = "3. Forståelig",
        guildeline_ = "3.2 Forutsigbar",
        contentGroup = "",
        tools = "Skjønn/DevTools",
    ),
    SuccessCriterion.createEmpty(
        number = "3.3.1",
        name = "3.3.1 Identifikasjon av feil",
        description =
        "Når en komponent kommer i fokus medfører dette ikke automatisk betydelige endringer i siden.",
        principle = "3. Forståelig",
        guildeline_ = "3.3 Inndatahjelp",
        contentGroup = "Skjemaer",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "3.3.2",
        name = "3.3.2 Ledetekster eller instruksjoner",
        description =
        "Endring av verdien til et skjemafelt medfører ikke automatisk betydelige endringer i siden.",
        principle = "3. Forståelig",
        guildeline_ = "3.3 Inndatahjelp",
        contentGroup = "Skjemaer",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "3.3.3",
        name = "3.3.3 Forslag ved feil",
        description =
        "Gi brukeren et tekstalternativ for innhold som ikke er tekst.",
        principle = "3. Forståelig",
        guildeline_ = "3.3 Inndatahjelp",
        contentGroup = "Skjemaer",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "3.3.4",
        name = "3.3.4 Forhindring av feil (juridiske feil, økonomiske feil, datafeil)",
        description =
        "Gi brukeren et alternativ når innholdet presenteres kun som video eller lyd.",
        principle = "3. Forståelig",
        guildeline_ = "3.3 Inndatahjelp",
        contentGroup = "Skjemaer",
        tools = "Skjønn",
    ),
    SuccessCriterion.createEmpty(
        number = "4.1.1",
        name = "4.1.1 Parsing (oppdeling)",
        description = "Tilby teksting for video med lyd.",
        principle = "4. Robust",
        guildeline_ = "4.1 Kompatibel",
        contentGroup = "",
        tools = "ARC Toolkit",
    ),
    SuccessCriterion.createEmpty(
        number = "4.1.2",
        name = "4.1.2 Navn, rolle, verdi",
        description =
        "Tilby en beskrivende tekst eller et lydspor med beskrivelse for videoer som ikke er direktesendt.",
        principle = "4. Robust",
        guildeline_ = "4.1 Kompatibel",
        contentGroup = "",
        tools = "ARC Toolkit/DevTools",
    ),
    SuccessCriterion.createEmpty(
        number = "4.1.3",
        name = "4.1.3 Statusbeskjeder",
        description =
        "Tilby synstolking til alle videoer som ikke er direktesendinger.",
        principle = "4. Robust",
        guildeline_ = "4.1 Kompatibel",
        contentGroup = "",
        tools = "Skjermleser/DevTools",
    ),
)
