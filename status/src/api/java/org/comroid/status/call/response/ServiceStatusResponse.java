package org.comroid.status.call.response;

import org.comroid.status.DependenyObject;
import org.comroid.status.call.StatusApiResponse;
import org.comroid.status.entity.Service;
import org.comroid.uniform.ValueType;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;

public interface ServiceStatusResponse extends StatusApiResponse {
    interface Bind extends StatusApiResponse.Bind {
        GroupBind<ServiceStatusResponse, DependenyObject> Root
                = new GroupBind<>(DependenyObject.SERIALIZATION_ADAPTER, "service_status_response");
        VarBind.DependentTwoStage<String, DependenyObject, org.comroid.status.entity.Service> Service
                = Root.bindDependent("name", ValueType.STRING, DependenyObject::resolveService);
        VarBind.TwoStage<Integer, Service.Status> Status
                = Root.bind2stage("status", ValueType.INTEGER, org.comroid.status.entity.Service.Status::valueOf);
    }
}
