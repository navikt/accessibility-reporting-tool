package accessibility.reporting.tool.microfrontends

import accessibility.reporting.tool.wcag.Status
import accessibility.reporting.tool.wcag.SuccessCriterion


fun compliant(id: String) = """<svg width="24" 
  role="img" 
  aria-labelledby="svg-sc${id}" 
  focusable="false"
class="compliant-icon icon" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
<title id="svg-sc${id}">Ok</title>
<path fill-rule="evenodd" clip-rule="evenodd" d="M18.998 6.94a.75.75 0 0 1 .063 1.058l-8 9a.75.75 0 0 1-1.091.032l-5-5a.75.75 0 1 1 1.06-1.06l4.438 4.437 7.471-8.405A.75.75 0 0 1 19 6.939Z" 
fill="#ffffff"></path>
</svg>"""

fun nonCompliant(id: String) =  """<svg width="24" 
  role="img" 
  aria-labelledby="svg-sc${id}" 
  focusable="false"
class="non-compliant-icon icon" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
<title id="svg-sc${id}">Avvik</title>
<path fill-rule="evenodd" clip-rule="evenodd" d="M6.53 5.47a.75.75 0 0 0-1.06 1.06L10.94 12l-5.47 5.47a.75.75 0 1 0 1.06 1.06L12 13.06l5.47 5.47a.75.75 0 1 0 1.06-1.06L13.06 12l5.47-5.47a.75.75 0 0 0-1.06-1.06L12 10.94 6.53 5.47Z" 
fill="#ffffff"></path>
</svg>"""

fun notTested(id: String) ="""<svg width="24" 
  role="img" 
  aria-labelledby="svg-sc${id}" 
  focusable="false"
  class="not-tested-icon icon" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <title id="svg-sc${id}">Ikke testet</title>
<path fill-rule="evenodd" clip-rule="evenodd" d="M9.75 9A2.25 2.25 0 0 1 12 6.75h.172a2.078 2.078 0 0 1 1.47 3.548l-1 1a4.75 4.75 0 0 0-1.392 3.359.75.75 0 0 0 1.5 0c0-.862.342-1.689.952-2.298l1-1a3.579 3.579 0 0 0-2.53-6.109H12A3.75 3.75 0 0 0 8.25 9v.5a.75.75 0 0 0 1.5 0V9ZM12 16.5a1 1 0 1 0 0 2 1 1 0 0 0 0-2Z" 
fill="#ffffff"></path>
</svg>"""

fun notApplicable(id: String) = """<svg width="24" 
  role="img" 
  aria-labelledby="svg-sc${id}"
  focusable="false"
  class="not-applicable-icon icon" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <title id="svg-sc${id}">Ikke aktuelt</title>
<path fill-rule="evenodd" clip-rule="evenodd" d="M4.75 12a.75.75 0 0 1 .75-.75h13a.75.75 0 1 1 0 1.5h-13a.75.75 0 0 1-.75-.75Z" 
fill="#ffffff"></path>
</svg>"""

fun toIcon(sc: SuccessCriterion): String = when (sc.status) {
    Status.COMPLIANT -> compliant(sc.number)
    Status.NON_COMPLIANT -> nonCompliant(sc.number)
    Status.NOT_APPLICABLE -> notApplicable(sc.number)
    Status.NOT_TESTED -> notTested(sc.number)
}