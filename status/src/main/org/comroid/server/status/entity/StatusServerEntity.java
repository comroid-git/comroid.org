package org.comroid.server.status.entity;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.IntStream;

import org.comroid.common.util.BitmaskUtil;
import org.comroid.server.status.StatusServer;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.varbind.GroupBind;
import org.comroid.varbind.VarBind;
import org.comroid.varbind.VariableCarrier;

import static org.comroid.uniform.adapter.json.fastjson.FastJSONLib.fastJsonLib;

public abstract class StatusServerEntity extends VariableCarrier<StatusServer> {
    private final Type entityType;

    protected StatusServerEntity(
            Type entityType, StatusServer server, UniObjectNode initialData
    ) {
        super(fastJsonLib, initialData, server);
        this.entityType = entityType;
    }

    public final StatusServer getServer() {
        return getDependencyObject();
    }

    public UUID getID() {
        return requireNonNull(Bind.ID);
    }

    public Type getType() {
        return entityType;
    }

    public static synchronized UUID nextID() {
        return UUID.randomUUID();
    }

    public interface Bind {
        GroupBind                 Root = new GroupBind(fastJsonLib, "entity");
        VarBind.Duo<String, UUID> ID   = Root.bind2stage("id", UniValueNode.ValueType.STRING, UUID::fromString);
    }

    public enum Type {
        SERVICE,

        MESSAGE;

        private final int mask;

        Type(Type... extendsTypes) {
            int[] maskParts = IntStream.concat(
                    IntStream.of(BitmaskUtil.nextFlag()),
                    Arrays.stream(extendsTypes)
                            .mapToInt(Type::getMask)
            )
                    .toArray();

            this.mask = BitmaskUtil.combine(maskParts);
        }

        public int getMask() {
            return mask;
        }

        public boolean isType(StatusServerEntity other) {
            return isType(other.getType());
        }

        public boolean isType(Type other) {
            return BitmaskUtil.isFlagSet(other.mask, mask);
        }

    }
}
