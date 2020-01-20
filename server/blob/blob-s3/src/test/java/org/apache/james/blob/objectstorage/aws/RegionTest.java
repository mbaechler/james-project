package org.apache.james.blob.objectstorage.aws;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class RegionTest {

    @Test
    void shouldNotAcceptNullRegion() {
        assertThatThrownBy(() -> Region.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRespectBeanContract() {
        EqualsVerifier.forClass(Region.class).verify();
    }

}