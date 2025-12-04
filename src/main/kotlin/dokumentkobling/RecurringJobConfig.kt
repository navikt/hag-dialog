package dokumentkobling

import no.nav.hag.utils.bakgrunnsjobb.RecurringJob

fun startRecurringJobs(jobber: List<RecurringJob>) {
    jobber.forEach {
        it.startAsync(true)
    }
}
