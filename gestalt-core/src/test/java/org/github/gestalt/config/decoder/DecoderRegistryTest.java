package org.github.gestalt.config.decoder;

import org.github.gestalt.config.annotations.ConfigPriority;
import org.github.gestalt.config.entity.ValidationError;
import org.github.gestalt.config.exceptions.GestaltConfigurationException;
import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.lexer.SentenceLexer;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.node.ConfigNodeService;
import org.github.gestalt.config.node.LeafNode;
import org.github.gestalt.config.node.NodeType;
import org.github.gestalt.config.path.mapper.PathMapper;
import org.github.gestalt.config.path.mapper.StandardPathMapper;
import org.github.gestalt.config.reflect.TypeCapture;
import org.github.gestalt.config.token.ArrayToken;
import org.github.gestalt.config.token.ObjectToken;
import org.github.gestalt.config.token.Token;
import org.github.gestalt.config.utils.ValidateOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

class DecoderRegistryTest {

    ConfigNodeService configNodeService;
    SentenceLexer lexer;

    @BeforeEach
    void setup() {
        configNodeService = Mockito.mock(ConfigNodeService.class);
        lexer = Mockito.mock(SentenceLexer.class);
    }

    @Test
    void getDecoder() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder()), configNodeService, lexer, List.of(new StandardPathMapper()));

        List<Decoder<?>> decoders = decoderRegistry.getDecoders();

        Assertions.assertEquals(4, decoders.size());
    }


    @Test
    void setDecoder() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder()), configNodeService, lexer, List.of(new StandardPathMapper()));

        List<Decoder<?>> decoders = decoderRegistry.getDecoders();

        Assertions.assertEquals(4, decoders.size());

        decoderRegistry.setDecoders(List.of(new DoubleDecoder(), new LongDecoder()));

        decoders = decoderRegistry.getDecoders();

        Assertions.assertEquals(2, decoders.size());
    }

    @Test
    void getDecoderForClass() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder()), configNodeService, lexer, List.of(new StandardPathMapper()));

        List<Decoder> decoders = decoderRegistry.getDecoderForClass(TypeCapture.of(String.class));

        Assertions.assertEquals(1, decoders.size());
        Assertions.assertTrue(decoders.get(0).matches(TypeCapture.of(String.class)));
    }

    @Test
    void addDecoderForClass() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(Collections.singletonList(new StringDecoder()), configNodeService, lexer,
            List.of(new StandardPathMapper()));

        List<Decoder> decoders = decoderRegistry.getDecoderForClass(TypeCapture.of(Double.class));

        Assertions.assertEquals(0, decoders.size());

        decoderRegistry.addDecoders(Collections.singletonList(new DoubleDecoder()));

        decoders = decoderRegistry.getDecoderForClass(TypeCapture.of(Double.class));

        Assertions.assertEquals(1, decoders.size());
        Assertions.assertTrue(decoders.get(0).matches(TypeCapture.of(Double.class)));
    }

    @Test
    void decoderRegistryPathMapperNull() {
        try {
            new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
                new StringDecoder()), configNodeService, lexer, null);
        } catch (GestaltException e) {
            Assertions.assertEquals("pathMappers can not be null or empty", e.getMessage());
        }
    }

    @Test
    void decoderRegistryPathMapperEmpty() {
        try {
            new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
                new StringDecoder()), configNodeService, lexer, List.of());
        } catch (GestaltException e) {
            Assertions.assertEquals("pathMappers can not be null or empty", e.getMessage());
        }
    }

    @Test
    void decoderRegistryConfigNodeNull() {
        try {
            new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
                new StringDecoder()), null, lexer, List.of(new StandardPathMapper()));
        } catch (GestaltException e) {
            Assertions.assertEquals("ConfigNodeService can not be null", e.getMessage());
        }
    }

    @Test
    void decoderLexerNull() {
        try {
            new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
                new StringDecoder()), configNodeService, null, List.of(new StandardPathMapper()));
        } catch (GestaltException e) {
            Assertions.assertEquals("SentenceLexer can not be null", e.getMessage());
        }
    }

    @Test
    void getDecoderForClassNull() {
        try {
            new DecoderRegistry(null, configNodeService, lexer, List.of(new StandardPathMapper()));
        } catch (GestaltException e) {
            Assertions.assertEquals("Decoder list was null or empty", e.getMessage());
        }
    }

    @Test
    void decoderRegistryGetAndSetPathMapper() throws GestaltConfigurationException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
                new StringDecoder()), configNodeService, lexer, List.of(new StandardPathMapper()));

        List<PathMapper> pathMappers = List.of(new StandardPathMapper(), new TestPathMapper());
        decoderRegistry.setPathMappers(pathMappers);
        Assertions.assertEquals(decoderRegistry.getPathMappers(), pathMappers);
    }

    @Test
    void getNextNodeObject() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder(), new LongDecoder()), configNodeService, lexer, List.of(new StandardPathMapper()));

        ConfigNode leaf = new LeafNode("test");

        Token nextToken = new ObjectToken("run");
        Mockito.when(lexer.scan("run")).thenReturn(ValidateOf.valid(Collections.singletonList(nextToken)));
        Mockito.when(configNodeService.navigateToNextNode("test", List.of(nextToken), leaf)).thenReturn(ValidateOf.valid(leaf));


        ValidateOf<ConfigNode> test = decoderRegistry.getNextNode("test", "run", leaf);
        Assertions.assertTrue(test.hasResults());
        Assertions.assertFalse(test.hasErrors());

        Assertions.assertEquals(leaf, test.results());

        Mockito.verify(lexer, Mockito.times(1)).scan(any());
        Mockito.verify(configNodeService, Mockito.times(1)).navigateToNextNode(any(), any(List.class), any());
    }

    @Test
    void decodeNode() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder()), configNodeService, lexer, List.of(new StandardPathMapper()));

        ConfigNode leaf = new LeafNode("value");

        ValidateOf<String> test = decoderRegistry.decodeNode("test", leaf, TypeCapture.of(String.class));
        Assertions.assertTrue(test.hasResults());
        Assertions.assertFalse(test.hasErrors());

        Assertions.assertEquals("value", test.results());
    }

    @Test
    void decodeNodeDuplicates() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder(), new LongDecoder()), configNodeService, lexer, List.of(new StandardPathMapper()));

        ConfigNode leaf = new LeafNode("100");

        ValidateOf<Long> test = decoderRegistry.decodeNode("test", leaf, TypeCapture.of(Long.class));
        Assertions.assertTrue(test.hasResults());
        Assertions.assertFalse(test.hasErrors());

        Assertions.assertEquals(100, test.results());
    }

    @Test
    void decodeNodeDuplicatesCustom() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder(), new LongDecoderCustomHigh(), new LongDecoderCustomVH()), configNodeService, lexer,
            List.of(new StandardPathMapper()));

        ConfigNode leaf = new LeafNode("100");

        ValidateOf<Long> test = decoderRegistry.decodeNode("test", leaf, TypeCapture.of(Long.class));
        Assertions.assertTrue(test.hasResults());
        Assertions.assertFalse(test.hasErrors());

        Assertions.assertEquals(1000L, test.results());
    }

    @Test
    void decodeNodeEmpty() {
        GestaltException ex = Assertions.assertThrows(GestaltException.class,
            () -> new DecoderRegistry(Collections.emptyList(), configNodeService, lexer, List.of(new StandardPathMapper())));
        Assertions.assertEquals("Decoder list was null or empty", ex.getMessage());

    }

    @Test
    void getNextNodeObjectBadToken() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder()), configNodeService, lexer, List.of(new StandardPathMapper()));

        ConfigNode leaf = new LeafNode("value");

        Token nextToken = new ObjectToken("run");
        Mockito.when(lexer.scan("run")).thenReturn(ValidateOf.inValid(new ValidationError.FailedToTokenizeElement("run", "test.run")));
        Mockito.when(configNodeService.navigateToNextNode("test", nextToken, leaf)).thenReturn(ValidateOf.valid(leaf));


        ValidateOf<ConfigNode> test = decoderRegistry.getNextNode("test", "run", leaf);
        Assertions.assertFalse(test.hasResults());
        Assertions.assertTrue(test.hasErrors());

        Assertions.assertEquals(1, test.getErrors().size());
        Assertions.assertEquals("Unable to tokenize element run for path: test.run", test.getErrors().get(0).description());

        Mockito.verify(lexer, Mockito.times(1)).scan(any());
        Mockito.verify(configNodeService, Mockito.times(0)).navigateToNextNode(any(), any(Token.class), any());
    }

    @Test
    void getNextNodeObjectNoResultToken() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder()), configNodeService, lexer, List.of(new StandardPathMapper()));

        ConfigNode leaf = new LeafNode("value");

        Token nextToken = new ObjectToken("run");
        Mockito.when(lexer.scan("run")).thenReturn(ValidateOf.validateOf(null, Collections.emptyList()));
        Mockito.when(configNodeService.navigateToNextNode("test", nextToken, leaf)).thenReturn(ValidateOf.valid(leaf));


        ValidateOf<ConfigNode> test = decoderRegistry.getNextNode("test", "run", leaf);
        Assertions.assertFalse(test.hasResults());
        Assertions.assertTrue(test.hasErrors());

        Assertions.assertEquals(1, test.getErrors().size());
        Assertions.assertEquals("Unable to find node matching path: test, for class: leaf, during decoding",
            test.getErrors().get(0).description());

        Mockito.verify(lexer, Mockito.times(1)).scan(any());
        Mockito.verify(configNodeService, Mockito.times(0)).navigateToNextNode(any(), any(Token.class), any());
    }

    @Test
    void getNextNodeArray() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder()), configNodeService, lexer, List.of(new StandardPathMapper()));

        ConfigNode leaf = new LeafNode("value");

        Token nextToken = new ArrayToken(1);
        Mockito.when(lexer.scan("run")).thenReturn(ValidateOf.valid(Collections.singletonList(nextToken)));
        Mockito.when(configNodeService.navigateToNextNode("test", List.of(nextToken), leaf)).thenReturn(ValidateOf.valid(leaf));


        ValidateOf<ConfigNode> test = decoderRegistry.getNextNode("test", 1, leaf);
        Assertions.assertTrue(test.hasResults());
        Assertions.assertFalse(test.hasErrors());

        Assertions.assertEquals(leaf, test.results());

        Mockito.verify(lexer, Mockito.times(0)).scan(any());
        Mockito.verify(configNodeService, Mockito.times(1)).navigateToNextNode(any(), any(List.class), any());
    }

    @Test
    void getNextNodeMultiPathMappers() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder()), configNodeService, lexer, List.of(new StandardPathMapper(), new TestPathMapper()));

        ConfigNode leaf = new LeafNode("test");

        Token nextToken = new ObjectToken("run");
        Mockito.when(lexer.scan("run")).thenReturn(ValidateOf.valid(Collections.singletonList(nextToken)));
        Mockito.when(configNodeService.navigateToNextNode("test", List.of(nextToken), leaf)).thenReturn(ValidateOf.valid(leaf));


        ValidateOf<ConfigNode> test = decoderRegistry.getNextNode("test", "run", leaf);
        Assertions.assertTrue(test.hasResults());
        Assertions.assertFalse(test.hasErrors());

        Assertions.assertEquals(leaf, test.results());

        Mockito.verify(lexer, Mockito.times(1)).scan(any());
        Mockito.verify(configNodeService, Mockito.times(1)).navigateToNextNode(any(), any(List.class), any());
    }

    @ConfigPriority(500)
    public class TestPathMapper implements PathMapper {
        @Override
        public ValidateOf<List<Token>> map(String path, String sentence, SentenceLexer lexer) {
            return ValidateOf.inValid(new ValidationError.NoResultsFoundForNode(path, NodeType.LEAF.getType(), "TestPathMapper"));
        }
    }
    @Test
    void getNextNodeMultiPathMappersFirstNoFind() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder()), configNodeService, lexer, List.of(new TestPathMapper(), new StandardPathMapper()));

        ConfigNode leaf = new LeafNode("test");

        Token nextToken = new ObjectToken("run");
        Mockito.when(lexer.scan("run")).thenReturn(ValidateOf.valid(Collections.singletonList(nextToken)));
        Mockito.when(configNodeService.navigateToNextNode("test", List.of(nextToken), leaf)).thenReturn(ValidateOf.valid(leaf));


        ValidateOf<ConfigNode> test = decoderRegistry.getNextNode("test", "run", leaf);
        Assertions.assertTrue(test.hasResults());
        Assertions.assertFalse(test.hasErrors());

        Assertions.assertEquals(leaf, test.results());

        Mockito.verify(lexer, Mockito.times(1)).scan(any());
        Mockito.verify(configNodeService, Mockito.times(1)).navigateToNextNode(any(), any(List.class), any());
    }

    @Test
    void getNextNodeMultiPathMappersNoneFind() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder()), configNodeService, lexer, List.of(new TestPathMapper(), new TestPathMapper()));

        ConfigNode leaf = new LeafNode("test");

        Token nextToken = new ObjectToken("run");
        Mockito.when(lexer.scan("run")).thenReturn(ValidateOf.valid(Collections.singletonList(nextToken)));
        Mockito.when(configNodeService.navigateToNextNode("test", List.of(nextToken), leaf)).thenReturn(ValidateOf.valid(leaf));


        ValidateOf<ConfigNode> test = decoderRegistry.getNextNode("test", "run", leaf);
        Assertions.assertFalse(test.hasResults());
        Assertions.assertTrue(test.hasErrors());

        Assertions.assertEquals(2, test.getErrors().size());
        Assertions.assertEquals("Unable to find node matching path: test, for class: leaf, during TestPathMapper",
            test.getErrors().get(0).description());
        Assertions.assertEquals("Unable to find node matching path: test, for class: leaf, during TestPathMapper",
            test.getErrors().get(1).description());

        Mockito.verify(lexer, Mockito.times(0)).scan(any());
        Mockito.verify(configNodeService, Mockito.times(0)).navigateToNextNode(any(), any(List.class), any());
    }

    @Test
    void getNextNodeMultiPathMappersNoneFindDuringNavigateToNextNode() throws GestaltException {
        DecoderRegistry decoderRegistry = new DecoderRegistry(List.of(new DoubleDecoder(), new LongDecoder(), new IntegerDecoder(),
            new StringDecoder()), configNodeService, lexer, List.of(new TestPathMapper(), new StandardPathMapper()));

        ConfigNode leaf = new LeafNode("test");

        Token nextToken = new ObjectToken("run");
        Mockito.when(lexer.scan("run")).thenReturn(ValidateOf.valid(Collections.singletonList(nextToken)));
        Mockito.when(configNodeService.navigateToNextNode("test", List.of(nextToken), leaf))
               .thenReturn(ValidateOf.inValid(new ValidationError.NoResultsFoundForNode("test", NodeType.LEAF.getType(), "navigate to next node")));


        ValidateOf<ConfigNode> test = decoderRegistry.getNextNode("test", "run", leaf);
        Assertions.assertFalse(test.hasResults());
        Assertions.assertTrue(test.hasErrors());

        Assertions.assertEquals(2, test.getErrors().size());
        Assertions.assertEquals("Unable to find node matching path: test, for class: leaf, during navigate to next node",
            test.getErrors().get(0).description());
        Assertions.assertEquals("Unable to find node matching path: test, for class: leaf, during TestPathMapper",
            test.getErrors().get(1).description());

        Mockito.verify(lexer, Mockito.times(1)).scan(any());
        Mockito.verify(configNodeService, Mockito.times(1)).navigateToNextNode(any(), any(List.class), any());
    }

    private static class LongDecoderCustomHigh extends LeafDecoder<Long> {

        @Override
        public Priority priority() {
            return Priority.HIGH;
        }

        @Override
        public String name() {
            return "LongDecoderCustom1";
        }

        @Override
        public boolean matches(TypeCapture<?> klass) {
            return Long.class.isAssignableFrom(klass.getRawType()) || long.class.isAssignableFrom(klass.getRawType());
        }


        @Override
        protected ValidateOf<Long> leafDecode(String path, ConfigNode node) {
            return ValidateOf.valid(10L);
        }
    }

    private static class LongDecoderCustomVH extends LeafDecoder<Long> {

        @Override
        public Priority priority() {
            return Priority.VERY_HIGH;
        }

        @Override
        public String name() {
            return "LongDecoderCustom1";
        }

        @Override
        public boolean matches(TypeCapture<?> klass) {
            return Long.class.isAssignableFrom(klass.getRawType()) || long.class.isAssignableFrom(klass.getRawType());
        }


        @Override
        protected ValidateOf<Long> leafDecode(String path, ConfigNode node) {
            return ValidateOf.valid(1000L);
        }
    }
}
