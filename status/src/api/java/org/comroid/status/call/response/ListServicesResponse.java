package org.comroid.status.call.response;

import org.comroid.common.iter.Span;
import org.comroid.status.DependenyObject;
import org.comroid.status.call.StatusApiResponse;
import org.comroid.status.entity.Service;
import org.comroid.uniform.ValueType;
import org.comroid.varbind.bind.ArrayBind;
import org.comroid.varbind.bind.GroupBind;

public interface ListServicesResponse extends StatusApiResponse {
    default Span<Service> getServices() {
        return requireNonNull(Bind.Services);
    }

    interface Bind extends StatusApiResponse.Bind {
        GroupBind<ListServicesResponse, DependenyObject> Root
                = new GroupBind<>(DependenyObject.SERIALIZATION_ADAPTER, "list_services_response");
        ArrayBind.DependentTwoStage<String, DependenyObject, Service, Span<Service>> Services
                = Root.listDependent("services", ValueType.STRING, DependenyObject::resolveService, Span::new);
    }
}
