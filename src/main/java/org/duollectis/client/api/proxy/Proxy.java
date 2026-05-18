package org.duollectis.client.api.proxy;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
public class Proxy {

	// ── persisted fields (serialised by Gson via @Expose) ─────────────────────
	@Setter @Expose private String name;
	@Setter @Expose private String address;
	@Setter @Expose private int    port;
	/** Null means type not yet determined (freshly imported, not checked). */
	@Setter @Expose @SerializedName("type") private Type type;
	@Setter @Expose private String username;
	@Setter @Expose private String password;

	// ── runtime fields (also persisted so state survives restarts) ────────────
	@Setter @Expose private volatile Status status    = Status.UNKNOWN;
	@Setter @Expose private volatile long latency = -1;
	@Setter @Expose private volatile int    pingCount = 0;

	// ── full constructor ──────────────────────────────────────────────────────
	public Proxy(String name, String address, int port, Type type, String username, String password) {
		this.name     = (name == null || name.isBlank()) ? address + ":" + port : name;
		this.address  = address;
		this.port     = port;
		this.type     = type;
		this.username = username;
		this.password = password;
	}

	/** Minimal constructor used by AddressParser (type unknown yet). */
	public Proxy(String address, int port) {
		this.name    = address + ":" + port;
		this.address = address;
		this.port    = port;
		this.type    = null; // unknown until checked
	}

	/**
	 * Called after Gson deserialisation to restore runtime defaults
	 * (Gson bypasses constructors via Unsafe, so volatile fields get JVM defaults).
	 */
	public void initRuntimeFields() {
		if (status == null)    status = Status.UNKNOWN;
		// If proxy was mid-check when saved, reset it back to UNKNOWN
		if (status == Status.CHECKING) status = Status.UNKNOWN;
		// ping=0 with pingCount=0 means Unsafe default (never set), restore to -1
		if (latency == 0 && pingCount == 0) latency = -1;
	}

	/** Returns true if this proxy is in any "failed" state (not OK, not UNKNOWN, not CHECKING). */
	public boolean isFailed() {
		return status == Status.FAILED
			|| status == Status.DEAD_IP
			|| status == Status.DEAD_SERVER
			|| status == Status.PING_ERROR;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Proxy proxy)) return false;
		return port == proxy.port && Objects.equals(address, proxy.address);
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, port);
	}

	// ── enums ─────────────────────────────────────────────────────────────────
	public enum Status {
		UNKNOWN     (0xFF666666),  // not yet checked
		CHECKING    (0xFFFFCC44),  // currently being checked
		OK          (0xFF44FF88),  // all 3 stages passed
		FAILED      (0xFFFF4444),  // generic failure (legacy / cancelled)
		DEAD_IP     (0xFFFF6622),  // stage 1 failed: TCP connect to proxy host timed out
		DEAD_SERVER (0xFFFF4488),  // stage 2 failed: SOCKS handshake rejected
		PING_ERROR  (0xFFFFAA00);  // stage 3 failed: could not reach target through proxy

		public final int color;
		Status(int color) { this.color = color; }
	}

	public enum Type {
		SOCKS4,
		SOCKS5,
	}
}
