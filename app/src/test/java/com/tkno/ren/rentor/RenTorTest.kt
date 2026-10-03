package com.tkno.ren.rentor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RenTorTest {

    @Test
    fun testBridgeTypeParsing() {
        assertEquals(BridgeType.NONE, BridgeType.fromCode("none"))
        assertEquals(BridgeType.SNOWFLAKE, BridgeType.fromCode("snowflake"))
        assertEquals(BridgeType.OBFS4, BridgeType.fromCode("obfs4"))
        assertEquals(BridgeType.MEEK_AZURE, BridgeType.fromCode("meek_azure"))
        assertEquals(BridgeType.WEBTUNNEL, BridgeType.fromCode("webtunnel"))
        assertEquals(BridgeType.CUSTOM, BridgeType.fromCode("custom"))
        assertEquals(BridgeType.NONE, BridgeType.fromCode("unknown_type"))
    }

    @Test
    fun testRenTorSettingsDefaultValues() {
        val settings = RenTorSettings()
        assertEquals(9050, settings.socksPort)
        assertEquals(9051, settings.controlPort)
        assertEquals(9053, settings.dnsPort)
        assertEquals(BridgeType.NONE, settings.bridgeType)
        assertTrue(settings.customBridges.isEmpty())
        assertNull(settings.preferredExitCountry)
        assertFalse(settings.strictNodes)
        assertTrue(settings.isolateDestPort)
        assertTrue(settings.isolateDestAddr)
        assertTrue(settings.blockGeolocation)
    }

    @Test
    fun testRenTorEngineStateTransitions() {
        val initial = RenTorEngineState()
        assertEquals(RenTorStatus.DISCONNECTED, initial.status)
        assertEquals(0, initial.bootstrapProgress)
        assertFalse(initial.isTorVerified)
        assertNull(initial.currentExitIp)

        val connecting = initial.copy(
            status = RenTorStatus.BOOTSTRAPPING,
            bootstrapProgress = 50,
            bootstrapMessage = "Loading relay descriptors 50%"
        )
        assertEquals(RenTorStatus.BOOTSTRAPPING, connecting.status)
        assertEquals(50, connecting.bootstrapProgress)

        val connected = connecting.copy(
            status = RenTorStatus.CONNECTED,
            bootstrapProgress = 100,
            currentExitIp = "185.220.101.5",
            isTorVerified = true
        )
        assertEquals(RenTorStatus.CONNECTED, connected.status)
        assertEquals(100, connected.bootstrapProgress)
        assertTrue(connected.isTorVerified)
        assertEquals("185.220.101.5", connected.currentExitIp)
    }

    @Test
    fun testRenTorCircuitModel() {
        val circuit = RenTorCircuit(
            id = "1",
            status = "BUILT",
            path = listOf("$AAAA~GuardNode", "$BBBB~MiddleNode", "$CCCC~ExitNode")
        )
        assertEquals("1", circuit.id)
        assertEquals("BUILT", circuit.status)
        assertEquals(3, circuit.path.size)
        assertEquals("$AAAA~GuardNode", circuit.path[0])
        assertEquals("$CCCC~ExitNode", circuit.path[2])
    }

    companion object {
        private const val AAAA = "0123456789ABCDEF0123456789ABCDEF01234567"
        private const val BBBB = "1123456789ABCDEF0123456789ABCDEF01234567"
        private const val CCCC = "2123456789ABCDEF0123456789ABCDEF01234567"
    }
}
