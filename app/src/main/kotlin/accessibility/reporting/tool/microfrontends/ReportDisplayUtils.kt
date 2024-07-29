package accessibility.reporting.tool.microfrontends


import accessibility.reporting.tool.wcag.criteria.Status
import accessibility.reporting.tool.wcag.criteria.SuccessCriterion.Companion.deviationCount
import accessibility.reporting.tool.wcag.criteria.SuccessCriterion.Companion.disputedDeviationCount
import accessibility.reporting.tool.wcag.report.Report
import accessibility.reporting.tool.wcag.report.ReportType

fun Report.h1() = when (reportType) {
    ReportType.AGGREGATED -> "Tilgjengelighetserklæring (Samlerapport)"
    ReportType.SINGLE -> "Tilgjengelighetserklæring"
}

fun Report.statusString(): String = when {
    successCriteria.any { it.status == Status.NOT_TESTED } -> "Ikke ferdig"
    successCriteria.deviationCount() != 0 ->
        "${successCriteria.deviationCount()} avvik, ${successCriteria.disputedDeviationCount().punkter} med merknad"

    successCriteria.deviationCount() == 0 ->
        "Ingen avvik, ${successCriteria.disputedDeviationCount().punkter} med merknad"

    else -> "Ukjent"
}

private val Int.punkter: String
    get() = if (this == 1) {
        "1 punkt"
    } else "$this punkter"