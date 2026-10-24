package com.example.ui.widget

import com.example.data.model.WidgetProviderItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPickerGroupingTest {
    private fun widget(pkg: String, app: String, label: String, cls: String) =
        WidgetProviderItem(
            providerComponent = "$pkg/$cls",
            packageName = pkg,
            className = cls,
            label = label,
            appName = app
        )

    @Test fun groupsByPackageAndSortsApplicationsWithoutChangingProviderOrder() {
        val clock1 = widget("com.clock", "Đồng hồ", "Số", ".Digital")
        val chrome = widget("com.chrome", "Chrome", "Tìm kiếm", ".Search")
        val clock2 = widget("com.clock", "Đồng hồ", "Kim", ".Analog")
        val groups = WidgetPickerGrouping.group(listOf(clock1, chrome, clock2))
        assertEquals(listOf("com.chrome", "com.clock"), groups.map { it.first })
        assertEquals(listOf(clock1, clock2), groups[1].second)
    }

    @Test fun searchMatchesWidgetLabelAppNameAndPackageCaseInsensitively() {
        val clock = widget("com.clock", "Đồng hồ", "Digital Clock", ".Digital")
        val chrome = widget("com.chrome", "Chrome", "Dấu trang", ".Bookmarks")
        val installed = listOf(clock, chrome)
        assertEquals(listOf(clock), WidgetPickerGrouping.group(installed, " DIGITAL ").flatMap { it.second })
        assertEquals(listOf(chrome), WidgetPickerGrouping.group(installed, "CHROME").flatMap { it.second })
        assertEquals(listOf(clock), WidgetPickerGrouping.group(installed, "com.clock").flatMap { it.second })
        assertTrue(WidgetPickerGrouping.group(installed, "không có").isEmpty())
    }

    @Test fun doesNotTruncateInstalledWidgetsOrMergeDifferentPackages() {
        val widgets = (1..224).map { index ->
            val pkg = if (index <= 112) "com.first" else "com.second"
            widget(pkg, if (index <= 112) "A" else "B", "Widget $index", ".Widget$index")
        }
        val groups = WidgetPickerGrouping.group(widgets)
        assertEquals(2, groups.size)
        assertEquals(224, groups.sumOf { it.second.size })
        assertEquals(112, groups[0].second.size)
        assertEquals(112, groups[1].second.size)
    }

    @Test fun emptyInputHasNoGroups() {
        assertTrue(WidgetPickerGrouping.group(emptyList()).isEmpty())
    }
}
