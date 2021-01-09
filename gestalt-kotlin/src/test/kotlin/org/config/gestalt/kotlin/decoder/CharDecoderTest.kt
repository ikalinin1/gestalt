package org.config.gestalt.kotlin.decoder

import org.config.gestalt.decoder.DecoderRegistry
import org.config.gestalt.entity.ValidationLevel
import org.config.gestalt.exceptions.GestaltException
import org.config.gestalt.kotlin.reflect.kTypeCaptureOf
import org.config.gestalt.lexer.SentenceLexer
import org.config.gestalt.node.ConfigNodeService
import org.config.gestalt.node.LeafNode
import org.config.gestalt.reflect.TypeCapture
import org.config.gestalt.utils.ValidateOf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*

internal class CharDecoderTest {
    var configNodeService: ConfigNodeService? = null
    var lexer: SentenceLexer? = null

    @BeforeEach
    fun setup() {
        configNodeService = Mockito.mock(ConfigNodeService::class.java)
        lexer = Mockito.mock(SentenceLexer::class.java)
    }

    @Test
    fun name() {
        val decoder = CharDecoder()
        Assertions.assertEquals("Character", decoder.name())
    }

    @Test
    fun matches() {
        val decoder = CharDecoder()
        Assertions.assertTrue(decoder.matches(kTypeCaptureOf<Char>()))
        Assertions.assertFalse(decoder.matches(object : TypeCapture<Char?>() {}))
        Assertions.assertFalse(decoder.matches(TypeCapture.of(Char::class.java)))
        Assertions.assertFalse(decoder.matches(kTypeCaptureOf<Int>()))
        Assertions.assertFalse(decoder.matches(kTypeCaptureOf<String>()))
        Assertions.assertFalse(decoder.matches(kTypeCaptureOf<Date>()))
        Assertions.assertFalse(decoder.matches(kTypeCaptureOf<List<Byte>>()))
    }

    @Test
    @Throws(GestaltException::class)
    fun decodeChar() {
        val decoder = CharDecoder()
        val validate: ValidateOf<Char> = decoder.decode(
            "db.port", LeafNode("a"), TypeCapture.of(
                Char::class.java
            ),
            DecoderRegistry(listOf(decoder), configNodeService, lexer)
        )
        Assertions.assertTrue(validate.hasResults())
        Assertions.assertFalse(validate.hasErrors())
        Assertions.assertEquals('a', validate.results())
        Assertions.assertEquals(0, validate.errors.size)
    }

    @Test
    @Throws(GestaltException::class)
    fun notACharTooLong() {
        val decoder = CharDecoder()
        val validate: ValidateOf<Char> = decoder.decode(
            "db.port", LeafNode("aaa"), TypeCapture.of(
                Char::class.java
            ),
            DecoderRegistry(listOf(decoder), configNodeService, lexer)
        )
        Assertions.assertTrue(validate.hasResults())
        Assertions.assertTrue(validate.hasErrors())
        Assertions.assertEquals('a', validate.results())
        Assertions.assertNotNull(validate.errors)
        Assertions.assertEquals(ValidationLevel.WARN, validate.errors[0].level())
        Assertions.assertEquals(
            "Expected a char on path: db.port, decoding node: LeafNode{value='aaa'} received the wrong size",
            validate.errors[0].description()
        )
    }

    @Test
    @Throws(GestaltException::class)
    fun notACharTooShort() {
        val decoder = CharDecoder()
        val validate: ValidateOf<Char> = decoder.decode(
            "db.port", LeafNode(""), TypeCapture.of(
                Char::class.java
            ),
            DecoderRegistry(listOf(decoder), configNodeService, lexer)
        )
        Assertions.assertFalse(validate.hasResults())
        Assertions.assertTrue(validate.hasErrors())
        Assertions.assertNull(validate.results())
        Assertions.assertNotNull(validate.errors)
        Assertions.assertEquals(
            "Expected a char on path: db.port, decoding node: LeafNode{value=''} received the wrong size",
            validate.errors[0].description()
        )
    }
}
