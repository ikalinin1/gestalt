package org.github.gestalt.config.path.mapper;

import org.github.gestalt.config.lexer.PathLexer;
import org.github.gestalt.config.token.ObjectToken;
import org.github.gestalt.config.token.Token;
import org.github.gestalt.config.utils.ValidateOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.github.gestalt.config.entity.ValidationLevel.ERROR;
import static org.github.gestalt.config.entity.ValidationLevel.MISSING_VALUE;

class SnakeCasePathMapperTest {

    @Test
    void map() {
        SnakeCasePathMapper mapper = new SnakeCasePathMapper();
        ValidateOf<List<Token>> results = mapper.map("my.path", "helloWorld", new PathLexer());

        Assertions.assertTrue(results.hasResults());
        Assertions.assertFalse(results.hasErrors());

        Assertions.assertEquals(1, results.results().size());
        Assertions.assertEquals(ObjectToken.class, results.results().get(0).getClass());
        Assertions.assertEquals("hello_world", ((ObjectToken) results.results().get(0)).getName());
    }

    @Test
    void mapNoChange() {
        SnakeCasePathMapper mapper = new SnakeCasePathMapper();
        ValidateOf<List<Token>> results = mapper.map("my.path", "hello", new PathLexer());

        Assertions.assertTrue(results.hasResults());
        Assertions.assertFalse(results.hasErrors());

        Assertions.assertEquals(1, results.results().size());
        Assertions.assertEquals(ObjectToken.class, results.results().get(0).getClass());
        Assertions.assertEquals("hello", ((ObjectToken) results.results().get(0)).getName());
    }

    @Test
    void mapMultiple() {
        SnakeCasePathMapper mapper = new SnakeCasePathMapper();
        ValidateOf<List<Token>> results = mapper.map("my.path", "helloWorldHowAreYou", new PathLexer());

        Assertions.assertTrue(results.hasResults());
        Assertions.assertFalse(results.hasErrors());

        Assertions.assertEquals(1, results.results().size());
        Assertions.assertEquals(ObjectToken.class, results.results().get(0).getClass());
        Assertions.assertEquals("hello_world_how_are_you", ((ObjectToken) results.results().get(0)).getName());
    }

    @Test
    void mapEmpty() {
        SnakeCasePathMapper mapper = new SnakeCasePathMapper();
        ValidateOf<List<Token>> results = mapper.map("my.path", "", new PathLexer());

        Assertions.assertFalse(results.hasResults());
        Assertions.assertTrue(results.hasErrors());

        Assertions.assertEquals(1, results.getErrors().size());
        Assertions.assertEquals(MISSING_VALUE, results.getErrors().get(0).level());
        Assertions.assertEquals("Unable to find node matching path: my.path, for class: MapNode, during Snake case path mapping",
            results.getErrors().get(0).description());
    }

    @Test
    void mapNull() {
        SnakeCasePathMapper mapper = new SnakeCasePathMapper();
        ValidateOf<List<Token>> results = mapper.map("my.path", null, new PathLexer());

        Assertions.assertFalse(results.hasResults());
        Assertions.assertTrue(results.hasErrors());

        Assertions.assertEquals(1, results.getErrors().size());
        Assertions.assertEquals(ERROR, results.getErrors().get(0).level());
        Assertions.assertEquals("Mapper: SnakeCasePathMapper key was null on path: my.path",
            results.getErrors().get(0).description());
    }

}
