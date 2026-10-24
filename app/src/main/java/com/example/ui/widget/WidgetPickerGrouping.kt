package com.example.ui.widget

import com.example.data.model.WidgetProviderItem
import java.util.Locale

/**
 * Pure grouping/search for native installed AppWidget providers.
 * Keep provider identity and input order inside each application.
 */
object WidgetPickerGrouping {
    fun group(
        installed: List<WidgetProviderItem>,
        query: String = ""
    ): List<Pair<String, List<WidgetProviderItem>>> {
        val search = query.trim().lowercase(Locale.ROOT)
        return installed.asSequence()
            .filter { widget ->
                search.isEmpty() ||
                    widget.label.lowercase(Locale.ROOT).contains(search) ||
                    widget.appName.lowercase(Locale.ROOT).contains(search) ||
                    widget.packageName.lowercase(Locale.ROOT).contains(search)
            }
            .groupBy { it.packageName }
            .toList()
            .sortedWith(compareBy(
                { (_, widgets) -> widgets.first().appName.lowercase(Locale.ROOT) },
                { (packageName, _) -> packageName }
            ))
    }
}
