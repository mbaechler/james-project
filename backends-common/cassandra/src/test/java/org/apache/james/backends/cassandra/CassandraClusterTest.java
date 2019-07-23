package org.apache.james.backends.cassandra;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CassandraClusterTest {

    @RegisterExtension
    static DockerCassandraExtension cassandraExtension = new DockerCassandraExtension();
    CassandraCluster cnx1;

    @BeforeEach
    void setUp() {
        cnx1 = createCluster();
    }

    @AfterEach
    void tearDown() {
        cnx1.close();
    }

    private CassandraCluster createCluster() {
        return CassandraCluster.create(CassandraModule.builder().build(), cassandraExtension.getDockerCassandra().getHost());
    }

    @Test
    void creatingTwoClustersShouldThrow() {
        assertThatThrownBy(this::createCluster).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void creatingTwoClustersSequentiallyShouldNotThrow() {
        cnx1.close();
        assertThatCode(() -> {
            try (CassandraCluster cluster = createCluster()) {

            }
        }).doesNotThrowAnyException();
    }

    @Test
    void closingAnAlreadyClosedConnectionShouldNotCloseANewOne() {
        cnx1.close();
        try (CassandraCluster cnx2 = createCluster()) {
            cnx1.close();
            assertThatThrownBy(this::createCluster).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void creatingTwoClustersShouldProvideFirstCreationStacktrace() {
        assertThatThrownBy(this::createCluster).hasStackTraceContaining("setUp");
    }
}