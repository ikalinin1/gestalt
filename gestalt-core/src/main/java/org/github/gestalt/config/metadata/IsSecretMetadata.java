package org.github.gestalt.config.metadata;

import java.util.List;
import java.util.Map;

public class IsSecretMetadata extends MetaDataValue<Boolean> {
    public static String SECRET = "secret";

    public IsSecretMetadata(Boolean value) {
        super(value);
    }

    @Override
    public String keyValue() {
        return SECRET;
    }

    // We do not want to rollup the IsSecretMetadata to the calling metadata
    @Override
    public Map<String, List<MetaDataValue<?>>> rollup(Map<String, List<MetaDataValue<?>>> metadata) {
        return metadata;
    }
}
