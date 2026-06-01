package org.duollectis.client.api.proxy;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Модель прокси-сервера: адрес, порт, тип (SOCKS4/5), учётные данные,
 * статус проверки и задержка. Поля статуса и задержки изменяются асинхронно
 * в ходе проверки {@link ProxyChecker}.
 */
@Getter
@ToString
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

	/** Минимальный конструктор, используемый {@link org.duollectis.client.utils.AddressParser} (тип неизвестен до проверки). */
	public Proxy(String address, int port) {
		this.name = address + ":" + port;
		this.address = address;
		this.port = port;
		this.type = null;
	}

	/**
	 * Восстанавливает runtime-поля после десериализации через Gson.
	 * Gson обходит конструкторы через Unsafe, поэтому volatile-поля получают JVM-умолчания.
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

	/** @return {@code true}, если прокси поддерживает одновременно SOCKS4 и SOCKS5 */
	public boolean isDualType() {
		return supportedTypes != null && supportedTypes.size() > 1;
	}

	/** @return {@code true}, если прокси не прошёл проверку (любой терминальный статус ошибки) */
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

	/** Статус проверки прокси. Каждый статус несёт цвет для отображения в GUI. */
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

	/** Протокол прокси-сервера. */
	public enum Type {
		SOCKS4,
		SOCKS5,
	}
}
