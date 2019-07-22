package org.apache.james.backends.cassandra.versions;

import java.util.Objects;

public class SchemaTransition {

    public static SchemaTransition to(SchemaVersion toVersion) {
        return new SchemaTransition(toVersion);
    }

    private final SchemaVersion toVersion;

    private SchemaTransition(SchemaVersion toVersion) {
        this.toVersion = toVersion;
    }

    public String fromAsString() {
        return String.valueOf(toVersion.previous().getValue());
    }


    public String toAsString() {
        return String.valueOf(toVersion.getValue());
    }

    public SchemaVersion from() {
        return toVersion.previous();
    }

    public SchemaVersion to() {
        return toVersion;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof SchemaTransition) {
            SchemaTransition that = (SchemaTransition) o;
            return Objects.equals(toVersion, that.toVersion);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(toVersion);
    }

    @Override
    public String toString() {
        return "SchemaTransition{" +
                "from " + fromAsString() + " to " + toAsString() +
                '}';
    }
}
