package com.doubao.tv.ui

import com.doubao.tv.ui.screen.Destination
import com.doubao.tv.ui.screen.settings.SettingsDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationDestinationTest {
    @Test
    fun `stored startup names resolve to typed destinations`() {
        assertEquals(Destination.Live, Destination.fromStoredName("Live"))
        assertEquals(Destination.Dashboard, Destination.fromStoredName("Dashboard/extra"))
        assertEquals(Destination.Loading, Destination.fromStoredName("Unknown"))
        assertEquals(Destination.Loading, Destination.fromStoredName(null))
    }

    @Test
    fun `legacy settings routes stay compatible`() {
        assertEquals(SettingsDestination.Categories, SettingsDestination.fromStoredName(null))
        assertEquals(SettingsDestination.App, SettingsDestination.fromStoredName("APP"))
        assertEquals(SettingsDestination.IptvSource, SettingsDestination.fromStoredName("IPTV_SOURCE/ignored"))
        assertEquals(SettingsDestination.Categories, SettingsDestination.fromStoredName("REMOVED"))
    }
}
