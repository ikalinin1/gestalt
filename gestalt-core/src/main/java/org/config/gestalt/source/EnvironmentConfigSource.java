package org.config.gestalt.source;

import org.config.gestalt.exceptions.GestaltException;
import org.config.gestalt.utils.Pair;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Convert the Environment Variables to a property file.
 * Apply the supplied transforms as we convert it.
 * Then write that as an input stream for the next stage in the parsing.
 */
public class EnvironmentConfigSource implements ConfigSource {

    public static final String ENV_VARS = "envVars";
    private final UUID id = UUID.randomUUID();

    public EnvironmentConfigSource() {
    }

    @Override
    public boolean hasStream() {
        return false;
    }

    @Override
    public InputStream loadStream() throws GestaltException {
        throw new GestaltException("Unsupported operation load stream on an EnvironmentConfigSource");
    }

    @Override
    public boolean hasList() {
        return true;
    }

    @Override
    public List<Pair<String, String>> loadList() {
        return System.getenv().entrySet()
            .stream()
            .map(envVar -> new Pair<>(envVar.getKey(), envVar.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    public String format() {
        return ENV_VARS;
    }

    @Override
    public String name() {
        return ENV_VARS;
    }

    @Override
    public UUID id() {  //NOPMD
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnvironmentConfigSource)) {
            return false;
        }
        EnvironmentConfigSource that = (EnvironmentConfigSource) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
