package org.apache.james.cli.probe.impl;

import java.io.IOException;

public interface JmxProbe {

    public JmxProbe connect(JmxConnection jmxc) throws IOException;

}
