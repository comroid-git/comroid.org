package org.comroid.status.call;

import org.comroid.status.DependenyObject;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainer;

public interface StatusApiResponse extends DataContainer<DependenyObject> {
    interface Bind {
        GroupBind<StatusApiResponse, DependenyObject> Root
                = new GroupBind<>(DependenyObject.SERIALIZATION_ADAPTER, "response");
    }
}
