package org.comroid.server.status.entity;

import java.util.UUID;

import org.comroid.common.ref.IntEnum;
import org.comroid.server.status.StatusServer;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.varbind.GroupBind;
import org.comroid.varbind.VarBind;
import org.comroid.varbind.VarCarrier;
import org.comroid.varbind.VariableCarrier;

public interface Entity extends VarCarrier<StatusServer> {
    UUID getID();

    Type getType();

    default StatusServer getServer() {
        return getDependencyObject();
    }

    interface Bind {
        GroupBind                  Root = new GroupBind(FastJSONLib.fastJsonLib, "entity");
        VarBind.Duo<String, UUID>  ID   = Root.bind2stage("id", UniValueNode.ValueType.STRING, UUID::fromString);
        VarBind.Duo<Integer, Type> Type = Root.bind2stage("type", UniValueNode.ValueType.INTEGER, Entity.Type::valueOf);
    }

    abstract class Abstract extends VariableCarrier<StatusServer> implements Entity {
        public Abstract(UniObjectNode initialData, StatusServer dependencyObject) {
            super(initialData, dependencyObject);
        }

        public Abstract(
                UniObjectNode initialData,
                StatusServer dependencyObject,
                Class<? extends VarCarrier<StatusServer>> containingClass
        ) {
            super(initialData, dependencyObject, containingClass);
        }

        @Override
        public UUID getID() {
            return requireNonNull(Bind.ID);
        }

        @Override
        public Type getType() {
            return requireNonNull(Bind.Type);
        }
    }

    enum Type implements IntEnum {
        SERVICE;

        @Override
        public int getValue() {
            return ordinal();
        }

        public static Type valueOf(int value) {
            return values()[value];
        }
    }
}
