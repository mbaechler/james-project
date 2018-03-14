package org.apache.james.domainlist.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.james.core.Domain;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class DomainTest {

    @Test
    public void shouldRespectBeanContract() {
        EqualsVerifier.forClass(Domain.class).verify();
    }

    @Test
    public void shouldBeCaseInsensitive() {
        assertThat(Domain.of("Domain")).isEqualTo(Domain.of("domain"));
    }

    @Test
    public void shouldThrowWhenDomainContainAtSymbol() {
        assertThatThrownBy(() -> Domain.of("Dom@in")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowWhenDomainIsEmpty() {
        assertThatThrownBy(() -> Domain.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowOnNullArgument() {
        assertThatThrownBy(() -> Domain.of(null)).isInstanceOf(NullPointerException.class);
    }

}