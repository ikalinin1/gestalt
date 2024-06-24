package org.github.gestalt.config.security.temporary;

import org.github.gestalt.config.annotations.ConfigPriority;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.node.LeafNode;
import org.github.gestalt.config.processor.config.ConfigNodeProcessor;
import org.github.gestalt.config.processor.config.ConfigNodeProcessorConfig;
import org.github.gestalt.config.secret.rules.SecretChecker;
import org.github.gestalt.config.utils.GResultOf;
import org.github.gestalt.config.utils.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks if the node is a leaf and a temporary secret. if it is, replaces the leaf node with a TemporaryLeafNode that can only be accessed
 * a limited number of times. After the limited number of times, the value is released to be GC'ed.
 *
 *  @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2024.
 */
@ConfigPriority(200)
public class TemporarySecretConfigNodeProcessor implements ConfigNodeProcessor {

    private final List<Pair<SecretChecker, Integer>> secretCounts = new ArrayList<>();

    private static final System.Logger logger = System.getLogger(TemporarySecretConfigNodeProcessor.class.getName());

    @Override
    public void applyConfig(ConfigNodeProcessorConfig config) {
        TemporarySecretModule moduleConfig = config.getConfig().getModuleConfig(TemporarySecretModule.class);

        if (moduleConfig == null) {
            logger.log(System.Logger.Level.DEBUG, "TemporarySecretModule has not been registered. " +
                "if you wish to use the TemporarySecretConfigNodeProcessor " +
                "then you must register an TemporarySecretModule config moduleConfig using the builder");
        } else {
            secretCounts.addAll(moduleConfig.getSecretCounts());
        }
    }

    @Override
    public GResultOf<ConfigNode> process(String path, ConfigNode currentNode) {
        var valueOptional = currentNode.getValue();
        if (!(currentNode instanceof LeafNode) || valueOptional.isEmpty()) {
            return GResultOf.result(currentNode);
        }

        var isTemporarySecret = secretCounts.stream()
            .filter(it -> it.getFirst().isSecret(path))
            .findFirst();

        // if this is not a temporary secret node, return the original node.
        if (isTemporarySecret.isEmpty()) {
            return GResultOf.result(currentNode);
        }

        Integer accessCount = isTemporarySecret.get().getSecond();

        return GResultOf.result(new TemporaryLeafNode((LeafNode) currentNode, accessCount));
    }
}
