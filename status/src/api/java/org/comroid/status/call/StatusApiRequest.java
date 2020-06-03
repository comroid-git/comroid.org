package org.comroid.status.call;

import org.comroid.status.DependenyObject;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainer;

public interface StatusApiRequest extends DataContainer<DependenyObject> {
    interface Bind {
        GroupBind<StatusApiRequest, DependenyObject> Root
                = new GroupBind<>(DependenyObject.Adapters.SERIALIZATION_ADAPTER, "request");
    }
}
