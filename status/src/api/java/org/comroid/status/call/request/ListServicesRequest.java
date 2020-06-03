package org.comroid.status.call.request;

import org.comroid.status.DependenyObject;
import org.comroid.status.call.StatusApiRequest;
import org.comroid.uniform.ValueType;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;

import java.util.regex.Pattern;

public interface ListServicesRequest extends StatusApiRequest {
    default Pattern getNameFilter() {
        return requireNonNull(Bind.NameFilter);
    }

    interface Bind extends StatusApiRequest.Bind {
        GroupBind<ListServicesRequest, DependenyObject> Root
                = new GroupBind<>(DependenyObject.Adapters.SERIALIZATION_ADAPTER, "list_services_request");
        VarBind.TwoStage<String, Pattern> NameFilter
                = Root.bind2stage("name_filter", ValueType.STRING, Pattern::compile);
    }
}
