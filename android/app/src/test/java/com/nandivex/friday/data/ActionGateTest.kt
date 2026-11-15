package com.nandivex.friday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionGateTest {
    private fun ask(gate: ActionGate, tool: DeviceTool, key: String, said: String = "") =
        gate.check(tool, key, "Send it?", said)

    private fun isAsk(d: ActionGate.Decision) = d is ActionGate.Decision.Ask

    @Test fun withNoOutsideTextEveryActionRunsAtOnce() {
        val gate = ActionGate()
        ActionGate.SENSITIVE_TOOLS.forEach { assertEquals(ActionGate.Decision.Allow, ask(gate, it, "name=rahul")) }
    }

    @Test fun anSmsAfterNotificationTextNeedsASpokenYes() = afterOutsideText(DeviceTool.READ_NOTIFICATIONS, DeviceTool.SEND_SMS)
    @Test fun anSmsAfterScreenTextNeedsASpokenYes() = afterOutsideText(DeviceTool.READ_SCREEN, DeviceTool.SEND_SMS)
    @Test fun anSmsAfterWebTextNeedsASpokenYes() = afterOutsideText(DeviceTool.FETCH_WEBPAGE, DeviceTool.SEND_SMS)
    @Test fun aCallAfterNotificationTextNeedsASpokenYes() = afterOutsideText(DeviceTool.READ_NOTIFICATIONS, DeviceTool.MAKE_CALL)
    @Test fun aCallAfterScreenTextNeedsASpokenYes() = afterOutsideText(DeviceTool.LOOK_AT_SCREEN, DeviceTool.MAKE_CALL)
    @Test fun aCallAfterWebTextNeedsASpokenYes() = afterOutsideText(DeviceTool.WEB_SEARCH, DeviceTool.MAKE_CALL)
    @Test fun aWhatsAppMessageAfterWebTextNeedsASpokenYes() = afterOutsideText(DeviceTool.WEB_SEARCH, DeviceTool.WHATSAPP_MESSAGE)
    @Test fun aShareAfterNotificationTextNeedsASpokenYes() = afterOutsideText(DeviceTool.READ_NOTIFICATIONS, DeviceTool.SHARE_TEXT)
    @Test fun aShareAfterScreenTextNeedsASpokenYes() = afterOutsideText(DeviceTool.READ_SCREEN, DeviceTool.SHARE_FILE)
    @Test fun aReplyAfterNotificationTextNeedsASpokenYes() = afterOutsideText(DeviceTool.READ_NOTIFICATIONS, DeviceTool.REPLY_NOTIFICATION)

    /** Reads outside text in turn one, is blocked, and only runs after a yes in turn two. */
    private fun afterOutsideText(reader: DeviceTool, action: DeviceTool) {
        assertTrue("$reader must count as outside text", reader in UntrustedContent.TOOLS)
        assertTrue("$action must be gated", action in ActionGate.SENSITIVE_TOOLS)
        val gate = ActionGate()
        gate.noteOutsideText()
        assertTrue(isAsk(ask(gate, action, "name=rahul|message=ok")))
        // The assistant asks, the turn ends, the user answers.
        gate.turnFinished()
        assertEquals(ActionGate.Decision.Allow, ask(gate, action, "name=rahul|message=ok", said = "yes"))
        // The yes is used up: a second identical call after fresh outside text is blocked again.
        gate.noteOutsideText()
        assertTrue(isAsk(ask(gate, action, "name=rahul|message=ok", said = "yes")))
    }

    @Test fun theUsersAnswerMustBeSpokenNotInferred() {
        val gate = ActionGate()
        gate.noteOutsideText()
        assertTrue(isAsk(ask(gate, DeviceTool.SEND_SMS, "name=rahul|message=ok")))
        gate.turnFinished()
        // The model calls again before any speech was transcribed: still blocked.
        assertTrue(isAsk(ask(gate, DeviceTool.SEND_SMS, "name=rahul|message=ok", said = "")))
    }

    @Test fun aNoOrAnythingElseCancelsTheAction() {
        listOf("no", "nahi", "don't send it", "cancel", "yes no wait", "what", "rahul ko bhi bhejo aur kal ka plan batao").forEach { said ->
            val gate = ActionGate()
            gate.noteOutsideText()
            ask(gate, DeviceTool.SEND_SMS, "name=rahul|message=ok")
            gate.turnFinished()
            assertTrue("\"$said\" must not allow it", isAsk(ask(gate, DeviceTool.SEND_SMS, "name=rahul|message=ok", said = said)))
        }
    }

    @Test fun aYesForADifferentActionDoesNotApprove() {
        val gate = ActionGate()
        gate.noteOutsideText()
        ask(gate, DeviceTool.SEND_SMS, "name=rahul|message=ok")
        gate.turnFinished()
        // Someone changed the message or the person: that is a different action and has not been agreed to.
        gate.noteOutsideText()
        assertTrue(isAsk(ask(gate, DeviceTool.SEND_SMS, "name=mummy|message=send money", said = "yes")))
        assertTrue(isAsk(ask(gate, DeviceTool.MAKE_CALL, "name=rahul|message=ok", said = "yes")))
    }

    @Test fun aNewTurnWithoutOutsideTextRunsFreely() {
        val gate = ActionGate()
        gate.noteOutsideText()
        gate.turnFinished()
        assertEquals(ActionGate.Decision.Allow, ask(gate, DeviceTool.SEND_SMS, "name=rahul|message=hello"))
    }

    @Test fun aQuestionThatIsNotAnsweredInTheNextTurnLapses() {
        val gate = ActionGate()
        gate.noteOutsideText()
        ask(gate, DeviceTool.SEND_SMS, "name=rahul|message=ok")
        gate.turnFinished()
        gate.turnFinished()
        // A stray "yes" much later cannot approve it.
        assertEquals(ActionGate.Decision.Allow, ask(gate, DeviceTool.SEND_SMS, "name=rahul|message=ok", said = "yes"))
        // Allowed only because no outside text is in that turn; with outside text it asks again.
        gate.noteOutsideText()
        assertTrue(isAsk(ask(gate, DeviceTool.SEND_SMS, "name=rahul|message=ok", said = "yes")))
    }

    @Test fun resetForgetsEverything() {
        val gate = ActionGate()
        gate.noteOutsideText()
        gate.reset()
        assertEquals(ActionGate.Decision.Allow, ask(gate, DeviceTool.SEND_SMS, "name=rahul|message=ok"))
    }

    @Test fun toolsThatReachNobodyAreNeverGated() {
        val gate = ActionGate()
        gate.noteOutsideText()
        listOf(DeviceTool.FLASHLIGHT, DeviceTool.OPEN_SMS_DRAFT, DeviceTool.DIAL_NUMBER, DeviceTool.END_CALL, DeviceTool.WEB_SEARCH).forEach {
            assertEquals("$it", ActionGate.Decision.Allow, ask(gate, it, "x"))
        }
    }

    @Test fun yesAndNoAreUnderstoodInEnglishAndHinglish() {
        listOf("yes", "Yes.", "yeah sure", "haan", "haan bhej do", "ok", "theek hai", "bilkul", "go ahead", "send").forEach {
            assertTrue("\"$it\" is a yes", ActionGate.isAffirmative(it))
        }
        listOf("", "no", "nahi", "haan nahi", "mat bhejo", "don't", "stop", "maybe later", "tell me more about it please now ok").forEach {
            assertFalse("\"$it\" is not a yes", ActionGate.isAffirmative(it))
        }
    }

    @Test fun everySensitiveToolBelongsToAConnector() {
        ActionGate.SENSITIVE_TOOLS.forEach { tool ->
            assertTrue("$tool", ConnectorRegistry.all.any { tool in it.tools })
        }
    }

    @Test fun textMessagesAreLimitedToTenAnHour() {
        val limit = SmsRateLimit()
        val start = 1_000_000L
        repeat(10) { assertTrue(limit.tryAcquire(start + it * 1000L)) }
        assertFalse(limit.tryAcquire(start + 20_000L))
        assertTrue("an hour later it works again", limit.tryAcquire(start + 3_700_000L))
    }

    @Test fun phoneNumbersAreCleanedForDialingAndWhatsApp() {
        assertEquals("+919876543210", PhoneNumbers.clean("+91 98765-43210"))
        assertNull(PhoneNumbers.clean("12"))
        assertNull(PhoneNumbers.clean(null))
        assertEquals("919876543210", PhoneNumbers.forWhatsApp("98765 43210"))
        assertEquals("919876543210", PhoneNumbers.forWhatsApp("09876543210"))
        assertEquals("919876543210", PhoneNumbers.forWhatsApp("+91 98765 43210"))
        assertEquals("447911123456", PhoneNumbers.forWhatsApp("0044 7911 123456"))
        assertNull(PhoneNumbers.forWhatsApp("123"))
        assertEquals("https://wa.me/919876543210?text=on%20my%20way%20%26%20late", PhoneNumbers.whatsAppLink("9876543210", "on my way & late"))
        assertEquals("https://wa.me/919876543210", PhoneNumbers.whatsAppLink("9876543210"))
    }

    @Test fun theNewCallAndMessageToolsLiveInOffByDefaultConnectors() {
        listOf("calls", "contacts", "sms", "whatsapp").forEach { id ->
            val c = ConnectorRegistry.get(id)
            assertEquals(ConnectorCategory.CALLS_MESSAGES, c.category)
            assertFalse("$id must be off on a fresh install", c.defaultOn)
        }
        assertEquals(
            setOf("make_call", "answer_call", "end_call", "reject_call", "set_speakerphone", "mute_call", "dial_number"),
            ConnectorRegistry.get("calls").tools.map { it.functionName }.toSet()
        )
        assertEquals(setOf("send_sms", "open_sms_draft"), ConnectorRegistry.get("sms").tools.map { it.functionName }.toSet())
        assertEquals(setOf("whatsapp_message", "whatsapp_call"), ConnectorRegistry.get("whatsapp").tools.map { it.functionName }.toSet())
    }
}
