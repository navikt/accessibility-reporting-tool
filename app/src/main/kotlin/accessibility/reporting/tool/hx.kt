package accessibility.reporting.tool

import kotlinx.html.HTMLTag


fun HTMLTag.hxPost(url: String) {
    attributes["data-hx-post"] = url
}

fun HTMLTag.hxGet(url: String) {
    attributes["data-hx-get"] = url
}
fun HTMLTag.hxTarget(selector: String) {
    attributes["data-hx-target"] = selector
}
fun HTMLTag.hxDelete(url: String) {
    attributes["data-hx-delete"] = url
}
fun HTMLTag.hxTrigger(rules: String) {
    attributes["data-hx-trigger"] = rules
}

fun HTMLTag.hxSelect(selector: String) {
    attributes["data-hx-select"] = selector

}

fun HTMLTag.hxSwapOuter() {
    attributes["data-hx-swap"] = "outerHTML"
}

fun HTMLTag.hxSwapInner() {
    attributes["data-hx-swap"] = "innerHTML"
}

fun HTMLTag.hxVals(json: String) {
    attributes["data-hx-vals"] = json
}
fun HTMLTag.hxOOB(selector: String) {
    attributes["data-hx-swap-oob"] = selector
}

fun HTMLTag.hxConfirm(message: String) {
    attributes["data-hx-confirm"] = message
}
