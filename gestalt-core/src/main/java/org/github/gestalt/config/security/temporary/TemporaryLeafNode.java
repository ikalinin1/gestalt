package org.github.gestalt.config.security.temporary;

import org.github.gestalt.config.lexer.PathLexer;
import org.github.gestalt.config.lexer.SentenceLexer;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.node.LeafNode;
import org.github.gestalt.config.node.NodeType;
import org.github.gestalt.config.secret.rules.SecretConcealer;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Temporary leaf node that holds a decorated leaf node.
 * Once the leaf value has been read the accessCount times, it will release the decorated node by setting it to null.
 * Eventually the decorated node should be garbage collected. but while waiting for GC it may still be found in memory.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2024.
 */
public class TemporaryLeafNode extends LeafNode {
    private final AtomicInteger accessCount;
    private LeafNode decoratedNode;

    public TemporaryLeafNode(LeafNode decoratedNode, int accessCount) {
        super("");
        this.accessCount = new AtomicInteger(accessCount);
        this.decoratedNode = decoratedNode;
    }

    @Override
    public Optional<String> getValue() {
        if (accessCount.get() > 0 && accessCount.getAndDecrement() > 0) {
            return decoratedNode.getValue();
        } else {
            decoratedNode = null;
            return Optional.empty();
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.LEAF;
    }

    @Override
    public Optional<ConfigNode> getIndex(int index) {
        return Optional.empty();
    }

    @Override
    public Optional<ConfigNode> getKey(String key) {
        return Optional.empty();
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TemporaryLeafNode)) {
            return false;
        }
        TemporaryLeafNode leafNode = (TemporaryLeafNode) o;
        return Objects.equals(decoratedNode.getValue(), leafNode.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(decoratedNode.getValue(), accessCount);
    }

    @Override
    public String toString() {
        // should not be used.
        return printer("", null, new PathLexer());
    }

    @Override
    public String printer(String path, SecretConcealer secretConcealer, SentenceLexer lexer) {
        String nodeValue;
        if (decoratedNode != null) {
            nodeValue = decoratedNode.getValue().orElse("");
        } else {
            nodeValue = "";
        }
        if (secretConcealer != null) {
            nodeValue = secretConcealer.concealSecret(path, nodeValue);
        }
        return "TemporaryLeafNode{" +
            "value='" + nodeValue + '\'' +
            "}";
    }
}
