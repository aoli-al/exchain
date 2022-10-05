package al.aoli.exchain.analyzer

import soot.tagkit.Host
import soot.tagkit.Tag

fun Host.addAll(result: Set<Tag>) {
    for (labelTag in result) {
        this.addTag(labelTag)
    }
}
