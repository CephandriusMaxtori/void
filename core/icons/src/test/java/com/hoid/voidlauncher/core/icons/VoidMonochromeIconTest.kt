package com.hoid.voidlauncher.core.icons

import org.junit.Assert.assertEquals
import org.junit.Test

class VoidMonochromeIconTest {
    @Test
    fun initials_useFirstTwoLettersForLabel() {
        assertEquals("AB", VoidMonochromeIcon.initialsForLabel("Alpha Beta"))
        assertEquals("A", VoidMonochromeIcon.initialsForLabel("A"))
        assertEquals("?", VoidMonochromeIcon.initialsForLabel("   "))
    }
}
