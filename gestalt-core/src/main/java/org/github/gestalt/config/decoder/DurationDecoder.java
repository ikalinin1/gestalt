package org.github.gestalt.config.decoder;

import org.github.gestalt.config.entity.ValidationError;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.reflect.TypeCapture;
import org.github.gestalt.config.tag.Tags;
import org.github.gestalt.config.utils.StringUtils;
import org.github.gestalt.config.utils.ValidateOf;

import java.time.Duration;

/**
 * Decode a duration.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
public final class DurationDecoder extends LeafDecoder<Duration> {

    @Override
    public Priority priority() {
        return Priority.MEDIUM;
    }

    @Override
    public String name() {
        return "Duration";
    }

    @Override
    public boolean canDecode(String path, Tags tags, ConfigNode node, TypeCapture<?> type) {
        return Duration.class.isAssignableFrom(type.getRawType());
    }

    @Override
    protected ValidateOf<Duration> leafDecode(String path, ConfigNode node) {
        ValidateOf<Duration> results;

        String value = node.getValue().orElse("");
        if (StringUtils.isInteger(value)) {
            try {
                long longVal = Long.parseLong(value);
                results = ValidateOf.valid(Duration.ofMillis(longVal));
            } catch (NumberFormatException e) {
                results = ValidateOf.inValid(new ValidationError.ErrorDecodingException(path, node, name()));
            }
        } else {
            results = ValidateOf.inValid(new ValidationError.DecodingNumberParsing(path, node, name()));
        }
        return results;
    }
}
