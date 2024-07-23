package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.wcag.SuccessCriterionInfo.Companion.operable
import accessibility.reporting.tool.wcag.SuccessCriterionInfo.Companion.perceivable
import accessibility.reporting.tool.wcag.SuccessCriterionInfo.Companion.robust
import accessibility.reporting.tool.wcag.SuccessCriterionInfo.Companion.understandable
import accessibility.reporting.tool.wcag.SucessCriteriaV1.ContentGroups.ikonerBilderGrafer
import accessibility.reporting.tool.wcag.SucessCriteriaV1.ContentGroups.lydVideoAnimasjoner
import accessibility.reporting.tool.wcag.SucessCriteriaV1.ContentGroups.skjema
import accessibility.reporting.tool.wcag.SucessCriteriaV1.ContentGroups.tastatur
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`1-1 Tekstalternativer`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`1-2 Tidsbaserte medier`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`1-3 Mulig å tilpasse`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`1-4 Mulig å skille fra hverandre`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`2-1 Tilgjengelig med tastatur`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`2-2 Nok tid`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`2-4 Navigerbar`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`2-5 Inndatametode`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`3-1 Leselig`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`3-2 Forutsigbar`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`3-3 Inndatahjelp`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`4-1 Kompatibel`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Tools.arcToolkit
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Tools.cca
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Tools.devTools
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Tools.skjønn
import accessibility.reporting.tool.wcag.WcagLevel.*
import mu.KotlinLogging
import java.time.LocalDateTime

val log = KotlinLogging.logger { }

object SucessCriteriaV1 {
    val lastTextUpdate: LocalDateTime = LocalDateTime.parse("2023-09-06T14:00:00.00")

    fun newReport(
        organizationUnit: OrganizationUnit?,
        reportId: String,
        url: String,
        user: User,
        descriptiveName: String
    ) = Report(
        organizationUnit = organizationUnit,
        reportId = reportId,
        successCriteria = criteriaTemplate,
        testData = null,
        url = url,
        author = user.toAuthor(),
        version = Version.V2,
        created = LocalDateTimeHelper.nowAtUtc(),
        lastChanged = LocalDateTimeHelper.nowAtUtc(),
        lastUpdatedBy = null,
        descriptiveName = descriptiveName,
        reportType = ReportType.SINGLE
    )

    private object ContentGroups {
        const val ikonerBilderGrafer = "Ikoner, bilder, grafer"
        const val lydVideoAnimasjoner = "Lyd, video, animasjoner"
        const val skjema = "Skjema"
        const val tastatur = "Tastatur"
    }

    object Tools {
        const val arcToolkit = "ARC Toolkit"
        const val skjønn = "Skjønn"
        const val devTools = "DevTools"
        const val cca = "CCA"
    }

    object Guidelines {
        const val `1-1 Tekstalternativer` = "1.1 Tekstalternativer"
        const val `1-2 Tidsbaserte medier` = "1.2 Tidsbaserte medier"
        const val `1-3 Mulig å tilpasse` = "1.3 Mulig å tilpasse"
        const val `1-4 Mulig å skille fra hverandre` = "1.4 Mulig å skille fra hverandre"
        const val `2-1 Tilgjengelig med tastatur` = "2.1 Tilgjengelig med tastatur"
        const val `2-2 Nok tid` = "2.2 Nok tid"
        const val `2-3 Anfall og andre fysiske reaksjoner` = "2.3 Anfall og andre fysiske reaksjoner"
        const val `2-4 Navigerbar` = "2.4 Navigerbar"
        const val `2-5 Inndatametode` = "2.5 Inndatametode"
        const val `3-1 Leselig` = "3.1 Leselig"
        const val `3-2 Forutsigbar` = "3.2 Forutsigbar"
        const val `3-3 Inndatahjelp` = "3.3 Inndatahjelp"
        const val `4-1 Kompatibel` = "4.1 Kompatibel"
    }

    val criteriaTemplate = listOf(
        //1.1 Non-text Content
        1.perceivable("1.1.1", "Ikke-tekstlig innhold") {
            description = "Gi brukeren et tekstalternativ for innhold som ikke er tekst."
            guideline = `1-1 Tekstalternativer`
            contentGroup = ikonerBilderGrafer
            tools = arcToolkit
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/non-text-content.html"
        }.levelA(),
        //1.2Time-based Media
        1.perceivable("1.2.1", "Bare lyd og bare video (forhåndsinnspilt)") {
            description = " Gi brukeren et alternativ når innholdet presenteres kun som video eller lyd."
            guideline = `1-2 Tidsbaserte medier`
            contentGroup = lydVideoAnimasjoner
            tools = skjønn
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/audio-only-and-video-only-prerecorded.html"
        }.levelA(),
        1.perceivable("1.2.2", "Teksting (forhåndsinnspilt)") {
            description = "Tilby teksting for forhåndsinnspilt video med lyd."
            guideline = `1-2 Tidsbaserte medier`
            contentGroup = lydVideoAnimasjoner
            tools = skjønn
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/captions-prerecorded"
        }.levelA(),
        1.perceivable("1.2.3", "Synstolking eller mediealternativ (forhåndsinnspilt)") {
            description =
                "Tilby en beskrivende tekst eller et lydspor med beskrivelse for videoer som ikke er direktesendt."
            guideline = `1-2 Tidsbaserte medier`
            contentGroup = lydVideoAnimasjoner
            tools = "Skjønn"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/audio-description-or-media-alternative-prerecorded"
        }.levelA(),
        1.perceivable("1.2.5", "Synstolking (forhåndsinnspilt)") {
            description = "Tilby synstolking til alle videoer som ikke er direktesendinger."
            guideline = `1-2 Tidsbaserte medier`
            contentGroup = lydVideoAnimasjoner
            tools = skjønn
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/audio-description-prerecorded"
        }.levelAA(),
        //1.3 Adaptable
        1.perceivable("1.3.1", "Informasjon og relasjoner") {
            description = "Ting skal være kodet som det ser ut som."
            guideline = `1-3 Mulig å tilpasse`
            tools = "$devTools/headingsMap"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/info-and-relationships"
        }.levelA(),
        1.perceivable("1.3.2", "Meningsbærende rekkefølge") {
            description = "Presenter innhold i en meningsfull rekkefølge."
            guideline = `1-3 Mulig å tilpasse`
            tools = "disableHTML"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/meaningful-sequence"
        }.levelA(),
        1.perceivable("1.3.3", "Sensoriske egenskaper") {
            description =
                "Instruksjoner må ikke utelukkende være avhengige av form, størrelse, visuell plassering, orientering, eller lyd for å kunne bli forstått."
            guideline = `1-3 Mulig å tilpasse`
            tools = skjønn
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/sensory-characteristics"
        }.levelA(),
        1.perceivable("1.3.4", "Visningsretning") {
            description = "Brukeren må få velge om innholdet skal vises i liggende eller stående retning."
            guideline = `1-3 Mulig å tilpasse`
            tools = devTools
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/orientation"
        }.levelA(),
        1.perceivable("1.3.5", "Identifiser formål med inndata") {
            description = "Skjemaelementer er kodet med inndataformål."
            guideline = `1-3 Mulig å tilpasse`
            contentGroup = skjema
            tools = devTools
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/identify-input-purpose"
        }.levelAA(),
        //1.4 Distinguishable
        1.perceivable("1.4.1", "Bruk av farge") {
            description = "Ikke bruk presentasjon som bygger utelukkende på farge."
            guideline = `1-4 Mulig å skille fra hverandre`
            tools = skjønn
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/distinguishable"
        }.levelA(),
        1.perceivable("1.4.2", "Styring av lyd") {
            description = "Gi brukeren mulighet til å stoppe eller pause lyd som starter automatisk."
            guideline = `1-4 Mulig å skille fra hverandre`
            contentGroup = lydVideoAnimasjoner
            tools = skjønn
            wcagUrl = "https://www.w3.org/TR/UNDERSTANDING-WCAG20/visual-audio-contrast-dis-audio.html"
        }.levelA(),
        1.perceivable("1.4.3", "Kontrast (minimum)") {
            description = "Kontrastforholdet mellom teksten og bakgrunnen er minst 4,5:1."
            guideline = `1-4 Mulig å skille fra hverandre`
            tools = "$arcToolkit/$cca"
            helpUrl = "https://aksel.nav.no/god-praksis/artikler/143-kontrast"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html"
        }.levelAA(),
        1.perceivable("1.4.4", "Endring av tekststørrelse") {
            description = "Tekst kan bli endret til 200% størrelse uten tap av innhold eller funksjon."
            guideline = `1-4 Mulig å skille fra hverandre`
            tools = "Zoom"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/resize-text"
        }.levelAA(),
        1.perceivable("1.4.5", " Bilder av tekst") {
            description = "Bruk tekst i stedet for bilder av tekst."
            guideline = `1-4 Mulig å skille fra hverandre`
            contentGroup = ikonerBilderGrafer
            tools = skjønn
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/images-of-text"
        }.levelAA(),
        1.perceivable("1.4.10", "Dynamisk tilpasning (Reflow)") {
            description =
                "Innhold skal kunne endres til 400 prosent størrelse ved 1280 piksler bredde, uten tap av informasjon eller funksjonalitet."
            guideline = `1-4 Mulig å skille fra hverandre`
            tools = "Zoom + $devTools"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/reflow.html"
        }.levelAA(),
        1.perceivable("1.4.11", "Kontrast for ikke-tekstlig innhold") {
            description =
                "Ikke-tekstlig innhold skal ha et kontrastforhold på minst 3:1 mot farge(r) som ligger ved siden av."
            guideline = `1-4 Mulig å skille fra hverandre`
            tools = cca
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html"
        }.levelAA(),
        1.perceivable("1.4.12", "Tekstavstand") {
            description = "Tekstavstanden kan overstyres for å gjøre teksten lettere å lese"
            guideline = `1-4 Mulig å skille fra hverandre`
            tools = arcToolkit
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/text-spacing"
            helpUrl = "https://aksel.nav.no/god-praksis/artikler/1412-tekstavstand"
        }.levelAA(),
        1.perceivable("1.4.13", "Pekerfølsomt innhold eller innhold ved tastaturfokus.") {
            description =
                "Brukeren skal ha mer kontroll over innholdet på nettsiden som får fokus med musepeker eller tastatur."
            guideline = `1-4 Mulig å skille fra hverandre`
            tools = skjønn
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/content-on-hover-or-focus"
        }.levelAA(),


        //2.1 Keyboard Accessible
        2.operable("2.1.1", "Tastatur") {
            description = "All funksjonalitet skal kunne brukes kun ved hjelp av tastatur."
            guideline = `2-1 Tilgjengelig med tastatur`
            tools = tastatur
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/keyboard"
        }.levelA(),
        2.operable("2.1.2", "Ingen tastaturfelle") {
            description = "Unngå tastaturfeller."
            guideline = `2-1 Tilgjengelig med tastatur`
            tools = tastatur
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/no-keyboard-trap"
        }.levelA(),
        2.operable("2.1.4", "Hurtigtaster som består av ett tegn") {
            description = "Brukeren skal enkelt kunne slå av hurtigtaster som består av ett tegn."

            guideline = `2-1 Tilgjengelig med tastatur`
            tools = "Skjønn"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/character-key-shortcuts"
        }.levelA(),
        //2.2 Enough Time
        2.operable("2.2.1", "Justerbar hastighet") {
            description = "Tidsbegrensninger skal kunne justeres av brukeren."
            guideline = `2-2 Nok tid`
            contentGroup = "Tidsbegrenset innhold"
            tools = "Skjønn"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/timing-adjustable"
        }.levelA(),
        2.operable("2.2.2", " Pause, stopp, skjul") {
            description = "Gi brukeren mulighet til å stoppe pause eller skjule innhold som automatisk endrer seg."
            guideline = `2-2 Nok tid`
            contentGroup = "Innhold som automatisk endrer seg eller oppdaterer"
            tools = "Skjønn"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/pause-stop-hide"
        }.levelA(),
        //2.3 Seizures and Physical Reactions
        2.operable("2.3.1", "Terskelverdi på maksimalt tre glimt") {
            description = "Innhold skal ikke blinke mer enn tre ganger per sekund."
            guideline = Guidelines.`2-3 Anfall og andre fysiske reaksjoner`
            contentGroup = "Innhold som blinker"
            tools = "Skjønn"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/three-flashes-or-below-threshold"
        }.levelA(),
        //2.4 Navigable
        2.operable("2.4.1", "Hoppe over blokker") {
            description = "Gi brukeren mulighet til å hoppe direkte til hovedinnholdet."
            guideline = `2-4 Navigerbar`
            tools = tastatur
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/bypass-blocks"
        }.levelA(),
        2.operable("2.4.2", "Sidetitler") {
            description = "Bruk nyttige og tydelige sidetitler."
            guideline = `2-4 Navigerbar`
            tools = "Skjønn"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/page-titled"
        }.levelA(),
        2.operable("2.4.3", "Fokusrekkefølge") {
            description = "Presenter innholdet i en logisk rekkefølge."
            guideline = `2-4 Navigerbar`
            tools = tastatur
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/focus-order"
        }.levelA(),
        2.operable("2.4.4", " Formål med lenke (i kontekst)") {
            description = "Alle lenkers mål og funksjon fremgår tydelig av lenketeksten."
            guideline = `2-4 Navigerbar`
            tools = devTools
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/link-purpose-in-context"
        }.levelA(),
        2.operable("2.4.5", "Flere måter") {
            description = "Tilby brukeren flere måter å navigere på."
            guideline = `2-4 Navigerbar`
            tools = "Skjønn"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/multiple-ways"
        }.levelAA(),
        2.operable("2.4.6", "Overskrifter og ledetekster") {
            description = "Sørg for at ledetekster og overskrifter er beskrivende."
            guideline = `2-4 Navigerbar`
            tools = "Skjønn/headingsMap"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/headings-and-labels"
        }.levelAA(),
        2.operable("2.4.7", " Synlig fokus") {
            description = "Sørg for at alt innhold får synlig fokus når du navigerer med tastatur."
            guideline = `2-4 Navigerbar`
            tools = tastatur
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/focus-visible"
        }.levelAA(),
        //2.5 Input Modalities
        2.operable("2.5.1", "Pekerbevegelser") {
            description =
                "Innhold på nettsiden skal kunne brukes med enkel pekerinput."
            guideline = `2-5 Inndatametode`
            tools = "Mus"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/pointer-gestures"
        }.levelA(),
        2.operable("2.5.2", "Pekeravbrytelse") {
            description = "Uheldige og feilaktige input via mus eller berøringsskjerm skal lettere kunne forhindres."
            guideline = `2-5 Inndatametode`
            tools = "Mus"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/pointer-cancellation"
        }.levelA(),
        2.operable("2.5.3", "Ledetekst i navn") {
            description = "Brukere som bruker visuelle ledetekster skal også kunne bruke kodede ledetekster."
            guideline = `2-5 Inndatametode`
            contentGroup = "Skjemaer"
            tools = devTools
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/label-in-name"
        }.levelA(),
        2.operable("2.5.4", "Bevegelsesaktivering") {
            description = "Funksjonalitet som kan betjenes med å bevege enheten eller ved brukerbevegelse, skal også kunne betjenes med brukergrensesnittkomponenter."
            guideline = `2-5 Inndatametode`
            tools = "Skjønn"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/motion-actuation"
        }.levelA(),
        //3.1 Readable
        3.understandable("3.1.1", "Språk på siden") {
            description = "Sørg for at språket til innholdet på alle nettsider er angitt i koden."
            guideline = `3-1 Leselig`
            tools = devTools
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/language-of-page"
        }.levelA(),
        3.understandable("3.1.2", "Språk på deler av innhold") {
            description =
                "Sørg for at alle deler av innholdet som er på et annet språk enn resten av siden er markert i koden."
            guideline = `3-1 Leselig`
            tools = devTools
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/language-of-parts"

        }.levelAA(),
        //3.2 Predictable
        3.understandable("3.2.1", "Fokus") {
            description = "Når en komponent kommer i fokus medfører dette ikke automatisk betydelige endringer i siden."
            guideline = `3-2 Forutsigbar`
            tools = tastatur
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/on-focus"
        }.levelA(),
        3.understandable("3.2.2", "Inndata") {
            description = "Endring av verdien til et skjemafelt medfører ikke automatisk betydelige endringer i siden."
            guideline = `3-2 Forutsigbar`
            contentGroup = "Skjemaer"
            tools = tastatur
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/on-input"
        }.levelA(),
        3.understandable("3.2.3", "Konsekvent navigering") {
            description = "Navigasjonslinker som gjentas på flere sider skal ha en konsekvent rekkefølge."
            guideline = `3-2 Forutsigbar`
            tools = "Skjønn"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/consistent-navigation"
        }.levelAA(),
        3.understandable("3.2.4", "Konsekvent identifikasjon") {
            description = "Elementer som har samme funksjonalitet på tvers av flere sider er utformet likt."
            guideline = `3-2 Forutsigbar`
            tools = "Skjønn/DevTools"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/consistent-identification"
        }.levelAA(),
        //3.3 Input Assistance
        3.understandable("3.3.1", "Identifikasjon av feil") {
            description =
                "For feil som oppdages automatisk må du vise hvor feilen har oppstått og gi en tekstbeskrivelse av feilen."
            guideline = `3-3 Inndatahjelp`
            contentGroup = skjema
            tools = skjønn
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/error-identification"
        }.levelA(),
        3.understandable("3.3.2", " Ledetekster eller instruksjoner") {
            description =
                "Det vises ledetekster eller instruksjoner når du har skjemaelementer som må fylles ut."
            guideline = `3-3 Inndatahjelp`
            contentGroup = skjema
            tools = skjønn
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/labels-or-instructions"
        }.levelA(),
        3.understandable("3.3.3", "Forslag ved feil") {
            description = "Dersom feil blir oppdaget automatisk, gi brukeren et forslag til hvordan feilen kan rettes."
            guideline = `3-3 Inndatahjelp`
            contentGroup = skjema
            tools = skjønn
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/error-suggestion"
        }.levelAA(),
        3.understandable("3.3.4", " Forhindring av feil (juridiske feil, økonomiske feil, datafeil)") {
            description =
                "For sider som medfører juridiske forpliktelser må det være mulig å kunne angre, kontrollere eller bekrefte dataene som sendes inn."
            guideline = `3-3 Inndatahjelp`
            contentGroup = skjema
            tools = skjønn
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/error-prevention-legal-financial-data"
        }.levelAA(),
        //4.1 Compatible
        4.robust("4.1.1", "Parsing (oppdeling)") {
            description = "Alle sider skal være uten store kodefeil."
            guideline = `4-1 Kompatibel`
            tools = arcToolkit
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/parsing"
        }.levelA(),
        4.robust("4.1.2", "Navn, rolle, verdi") {
            description = "Alle komponenter har navn og rolle bestemt i koden."
            guideline = `4-1 Kompatibel`
            tools = "ARC Toolkit/DevTools"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/name-role-value"
        }.levelA(),
        4.robust("4.1.3", "Statusbeskjeder") {
            description = "Brukeren skal få statusbeskjeder om viktige endringer på nettsiden uten at det gir kontekstendring."
            guideline = `4-1 Kompatibel`
            tools = "Skjermleser/$devTools"
            helpUrl = "https://aksel.nav.no/god-praksis/artikler/413-statusbeskjeder#hcdb4fcfaf29c"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/status-messages.html"
        }.levelA()
    )

    fun updateCriterion(originalSuccessCriterion: SuccessCriterion): SuccessCriterion {
        val templateCriteria = criteriaTemplate
            .find { it.number.trimEnd() == originalSuccessCriterion.number.trimEnd() }
        return if (templateCriteria == null) {
            log.warn { "Fant ikke suksesskriterie med nummer ${originalSuccessCriterion.number} i Version1.criteria liste" }
            originalSuccessCriterion
        } else {
            //TODO: legg til andre felt som evt blir oppdatert
            originalSuccessCriterion.copy(
                name = templateCriteria.name,
                description = templateCriteria.description,
                wcagUrl = templateCriteria.wcagUrl,
                helpUrl = templateCriteria.helpUrl
            ).apply {
                wcagLevel = templateCriteria.wcagLevel
            }
        }
    }

}

fun SuccessCriterion.levelA() =
    apply { wcagLevel = A }


private fun SuccessCriterion.levelAA() =
    apply { wcagLevel = AA }

