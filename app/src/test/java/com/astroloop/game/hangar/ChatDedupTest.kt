package com.astroloop.game.hangar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class ChatDedupTest {

    // Mirror of the dedup helper we'll add to HangarState
    private fun addIfNotDuplicate(
        list: CopyOnWriteArrayList<ChatMessage>,
        msg: ChatMessage,
        windowSize: Int = 10
    ) {
        val recent = list.takeLast(windowSize)
        if (recent.none { it.text == msg.text }) {
            list.add(msg)
        }
    }

    @Test
    fun `same text is not added twice to visible window`() {
        val messages = CopyOnWriteArrayList<ChatMessage>()
        val msg = ChatMessage("TB-26", "Hello commander.", 0xFFFFFFFF.toInt())
        addIfNotDuplicate(messages, msg)
        addIfNotDuplicate(messages, msg)
        assertEquals(1, messages.size)
    }

    @Test
    fun `different text is always added`() {
        val messages = CopyOnWriteArrayList<ChatMessage>()
        addIfNotDuplicate(messages, ChatMessage("A", "Line one.", 0xFFFFFFFF.toInt()))
        addIfNotDuplicate(messages, ChatMessage("B", "Line two.", 0xFFFFFFFF.toInt()))
        assertEquals(2, messages.size)
    }

    @Test
    fun `same text from different speakers is still deduplicated`() {
        val messages = CopyOnWriteArrayList<ChatMessage>()
        addIfNotDuplicate(messages, ChatMessage("TB-26", "Someone's watching.", 0xFFFFFFFF.toInt()))
        addIfNotDuplicate(messages, ChatMessage("RASCAL", "Someone's watching.", 0xFFFFFFFF.toInt()))
        assertEquals(1, messages.size)
    }
}
