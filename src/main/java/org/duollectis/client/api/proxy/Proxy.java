package org.duollectis.client.api.proxy;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;
import java.util.Objects;

@Getter
public class Proxy {

    @Setter
    @Expose
    private String name;

    @Setter
    @Expose
    private String address;

    @Setter
    @Expose
    private int port;

    @Setter
    @Expose
    @SerializedName("type")
    private Type type;

    @Setter
    @Expose
    @SerializedName("supportedTypes")
    private EnumSet<Type> supportedTypes;

    @Setter
    @Expose
    private String username;

    @Setter
    @Expose
    private String password;

    @Setter
    @Expose
    private volatile Status status = Status.UNKNOWN;

    @Setter
    @Expose
    private volatile long latency = -1;

    @Setter
    @Expose
    private volatile int pingCount = 0;

    public Proxy(String name, String address, int port, Type type, String username, String password) {
        this.name = (name == null || name.isBlank()) ? address + ":" + port : name;
        this.address = address;
        this.port = port;
        this.type = type;
        this.username = username;
        this.password = password;
    }

    /** Minimal constructor used by AddressParser (type unknown until checked). */
    public Proxy(String address, int port) {
        this.name = address + ":" + port;
        this.address = address;
        this.port = port;
        this.type = null;
    }

    /**
     * Restores runtime defaults after Gson deserialisation.
     * Gson bypasses constructors via Unsafe, so volatile fields get JVM defaults.
     */
    public void initRuntimeFields() {
        if (status == null || status == Status.CHECKING) {
            status = Status.UNKNOWN;
        }
        if (latency == 0 && pingCount == 0) {
            latency = -1;
        }
        if (supportedTypes == null) {
            supportedTypes = type != null ? EnumSet.of(type) : EnumSet.noneOf(Type.class);
        }
    }

    /** True if this proxy supports both SOCKS4 and SOCKS5. */
    public boolean isDualType() {
        return supportedTypes != null && supportedTypes.size() > 1;
    }

    /** True if this proxy is in any terminal failure state. */
    public boolean isFailed() {
        return switch (status) {
            case FAILED, DEAD_IP, DEAD_SERVER, PING_ERROR -> true;
            default -> false;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Proxy other)) {
            return false;
        }
        return port == other.port && Objects.equals(address, other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

    public enum Status {
        UNKNOWN(0xFF666666),
        CHECKING(0xFFFFCC44),
        OK(0xFF44FF88),
        FAILED(0xFFFF4444),
        DEAD_IP(0xFFFF6622),
        DEAD_SERVER(0xFFFF4488),
        PING_ERROR(0xFFFFAA00);

        public final int color;

        Status(int color) {
            this.color = color;
        }
    }

    public enum Type {
        SOCKS4,
        SOCKS5,
    }
}
