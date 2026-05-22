package org.duollectis.client.api.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Асинхронный проверщик прокси-серверов.
 * Использует виртуальные потоки (Project Loom) для параллельной проверки.
 * Алгоритм: определяет поддерживаемые типы (SOCKS4/5) через рукопожатие,
 * затем опционально пингует через MC-протокол для измерения задержки.
 * Настройки сохраняются на диск через {@link #saveSettings()} и восстанавливаются через {@link #loadSettings()}.
 */
public class ProxyChecker {

	private static final int SEGMENT_BITS = 0x7F;
	private static final int CONTINUE_BIT = 0x80;

	@Getter
	private static final ProxyChecker instance = new ProxyChecker();

	@Getter
	private Settings settings = new Settings();

	private ProxyChecker() {}

	public ProxyChecker maxPings(int v) {
		settings.maxPings = Math.max(1, v);
		return this;
	}

	public ProxyChecker maxDelayMs(int v) {
		settings.maxDelayMs = Math.max(100, v);
		return this;
	}

	public ProxyChecker parallelism(int v) {
		settings.parallelism = Math.max(1, v);
		return this;
	}

	public ProxyChecker pingStage3(boolean v) {
		settings.pingStage3 = v;
		return this;
	}

	public ProxyChecker target(String host, int port) {
		if (host != null && !host.isBlank()) {
			settings.targetHost = host.trim();
		}

		if (port > 0 && port <= 65535) {
			settings.targetPort = port;
		}

		return this;
	}


	private static final Gson SETTINGS_GSON = new GsonBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			.setPrettyPrinting()
			.create();

	@Getter
	public static class Settings {

		@Expose
		private String targetHost = "mc.hypixel.net";

		@Expose
		private int targetPort = 25565;

		@Expose
		private boolean pingStage3 = true;

		@Expose
		private int maxPings = 3;

		@Expose
		private int maxDelayMs = 5000;

		@Expose
		private int parallelism = 64;

		@Expose
		private String filterMode = "ALL";

		@Expose
		private String sortMode = "NONE";

		public void setFilterMode(String filterMode) {
			this.filterMode = filterMode;
		}

		public void setSortMode(String sortMode) {
			this.sortMode = sortMode;
		}
	}

	public void saveSettings() {
		File file = settingsFile();
		file.getParentFile().mkdirs();

		try (FileWriter w = new FileWriter(file)) {
			SETTINGS_GSON.toJson(settings, w);
		}
		catch (IOException ignored) {}
	}

	public void loadSettings() {
		File file = settingsFile();

		if (!file.exists()) {
			return;
		}

		try (FileReader r = new FileReader(file)) {
			settings = SETTINGS_GSON.fromJson(r, Settings.class);
		}
		catch (IOException ignored) {}
	}

	private static File settingsFile() {
		return new File(
				FabricLoader.getInstance().getGameDir().toFile(),
				"duollectis/checker_settings.json"
		);
	}

	@Getter
	private volatile boolean running = false;

	private final AtomicInteger checked = new AtomicInteger(0);
	private final AtomicInteger total = new AtomicInteger(0);
	private final AtomicBoolean cancelled = new AtomicBoolean(false);
	private ExecutorService executor;
	private Semaphore semaphore;

	/**
	 * Запускает асинхронную проверку прокси со статусом {@link Proxy.Status#UNKNOWN}.
	 * Если нет прокси для проверки или проверка уже идёт, вызов игнорируется.
	 *
	 * @param protocolVersion версия протокола Minecraft для ping-стадии
	 * @param proxies         список прокси для проверки
	 */
	public synchronized void start(int protocolVersion, Collection<Proxy> proxies) {
		List<Proxy> pending = proxies.stream()
				.filter(p -> p.getStatus() == Proxy.Status.UNKNOWN)
				.toList();

		if (pending.isEmpty()) {
			return;
		}

		startInternal(protocolVersion, pending);
	}

	/**
	 * Отменяет текущую проверку: прерывает пул потоков и сбрасывает флаг выполнения.
	 */
	public synchronized void cancel() {
		cancelled.set(true);

		if (executor != null) {
			executor.shutdownNow();
		}

		running = false;
	}

	private void startInternal(int protocolVersion, Collection<Proxy> proxies) {
		if (running) {
			return;
		}

		running = true;
		cancelled.set(false);
		checked.set(0);
		total.set(proxies.size());
		semaphore = new Semaphore(settings.parallelism);
		executor = Executors.newVirtualThreadPerTaskExecutor();

		for (Proxy proxy : proxies) {
			executor.submit(() -> {
				try {
					semaphore.acquire();

					try {
						checkProxy(protocolVersion, proxy);
					}
					finally {
						semaphore.release();
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();

					if (proxy.getStatus() == Proxy.Status.CHECKING) {
						proxy.setStatus(Proxy.Status.UNKNOWN);
					}
				}
			});
		}

		Thread.ofVirtual().name("proxy-checker-watcher").start(() -> {
			executor.shutdown();

			try {
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException ignored) {}

			running = false;
			ProxyManager pm = ProxyManager.getInstance();

			if (pm != null) {
				pm.save();
			}
		});
	}

	/**
	 * Выполняет полную проверку одного прокси: TCP-соединение → определение типов → ping через MC-протокол.
	 * Результат записывается напрямую в поля объекта {@link Proxy}.
	 *
	 * @param protocolVersion версия протокола Minecraft для ping-стадии
	 * @param proxy           проверяемый прокси-объект
	 */
	private void checkProxy(int protocolVersion, Proxy proxy) {
		if (cancelled.get()) {
			if (proxy.getStatus() == Proxy.Status.CHECKING) {
				proxy.setStatus(Proxy.Status.UNKNOWN);
			}

			checked.incrementAndGet();
			return;
		}

		proxy.setStatus(Proxy.Status.CHECKING);
		proxy.setPingCount(0);
		proxy.setLatency(-1);

		long stageStart = System.currentTimeMillis();
		Socket probeSocket = new Socket();

		try {
			probeSocket.connect(new InetSocketAddress(proxy.getAddress(), proxy.getPort()), settings.maxDelayMs);
			probeSocket.setSoTimeout(settings.maxDelayMs);
		}
		catch (IOException e) {
			closeQuietly(probeSocket);
			proxy.setStatus(cancelled.get() ? Proxy.Status.UNKNOWN : Proxy.Status.DEAD_IP);
			checked.incrementAndGet();
			return;
		}

		EnumSet<Proxy.Type> foundTypes = EnumSet.noneOf(Proxy.Type.class);

		for (int attempt = 0; attempt < settings.maxPings && !cancelled.get(); attempt++) {
			try {
				foundTypes.addAll(detectAllTypes(proxy.getAddress(), proxy.getPort()));
			} catch (IOException ignored) {}

			if (!foundTypes.isEmpty()) {
				break;
			}
		}

		closeQuietly(probeSocket);

		if (foundTypes.isEmpty()) {
			proxy.setStatus(cancelled.get() ? Proxy.Status.UNKNOWN : Proxy.Status.DEAD_SERVER);
			checked.incrementAndGet();
			return;
		}

		Proxy.Type primaryType = foundTypes.contains(Proxy.Type.SOCKS5) ? Proxy.Type.SOCKS5 : Proxy.Type.SOCKS4;

		if (!settings.pingStage3) {
			proxy.setType(primaryType);
			proxy.setSupportedTypes(foundTypes);
			proxy.setLatency(System.currentTimeMillis() - stageStart);
			proxy.setPingCount(1);
			proxy.setStatus(Proxy.Status.OK);
			checked.incrementAndGet();
			return;
		}

		int successCount = 0;
		long totalPing = 0;

		for (int attempt = 0; attempt < settings.maxPings && !cancelled.get(); attempt++) {
			long ping = pingThroughProxy(protocolVersion, proxy.getAddress(), proxy.getPort(), primaryType);

			if (ping >= 0) {
				successCount++;
				totalPing += ping;
			}
		}

		if (cancelled.get()) {
			proxy.setStatus(Proxy.Status.UNKNOWN);
		}
		else if (successCount > 0) {
			proxy.setType(primaryType);
			proxy.setSupportedTypes(foundTypes);
			proxy.setLatency(totalPing / successCount);
			proxy.setPingCount(successCount);
			proxy.setStatus(Proxy.Status.OK);
		}
		else {
			proxy.setType(primaryType);
			proxy.setSupportedTypes(foundTypes);
			proxy.setStatus(Proxy.Status.PING_ERROR);
		}

		checked.incrementAndGet();
	}

	/**
	 * Определяет поддерживаемые типы прокси (SOCKS4/SOCKS5) через отдельные рукопожатия.
	 * Каждый тип проверяется независимым TCP-соединением.
	 *
	 * @param host адрес прокси-сервера
	 * @param port порт прокси-сервера
	 * @return множество поддерживаемых типов; пустое, если прокси не отвечает ни на один протокол
	 * @throws IOException при критической ошибке сети (не перехватывается внутри)
	 */
	private EnumSet<Proxy.Type> detectAllTypes(String host, int port) throws IOException {
		EnumSet<Proxy.Type> result = EnumSet.noneOf(Proxy.Type.class);

		try (Socket s5 = new Socket()) {
			s5.connect(new InetSocketAddress(host, port), settings.maxDelayMs);
			s5.setSoTimeout(settings.maxDelayMs);
			var out5 = s5.getOutputStream();
			var in5 = s5.getInputStream();
			out5.write(new byte[]{0x05, 0x01, 0x00});
			out5.flush();
			byte[] resp = new byte[2];

			if (readFully(in5, resp, 2) == 2 && resp[0] == 0x05 && (resp[1] & 0xFF) != 0xFF) {
				result.add(Proxy.Type.SOCKS5);
			}
		}
		catch (IOException ignored) {}

		try (Socket s4 = new Socket()) {
			s4.connect(new InetSocketAddress(host, port), settings.maxDelayMs);
			s4.setSoTimeout(settings.maxDelayMs);
			byte[] req = {0x04, 0x01, 0x00, 0x50, 0x01, 0x01, 0x01, 0x01, 0x00};
			s4.getOutputStream().write(req);
			s4.getOutputStream().flush();
			byte[] r4 = new byte[8];
			int read = readFully(s4.getInputStream(), r4, 8);

			if (read >= 2 && r4[0] == 0x00 && r4[1] == 0x5A) {
				result.add(Proxy.Type.SOCKS4);
			}
		}
		catch (IOException ignored) {}

		return result;
	}

	/**
	 * Измеряет задержку до целевого MC-сервера через прокси.
	 * Устанавливает туннель SOCKS4/5, отправляет MC Handshake + Status Request и ждёт первого байта ответа.
	 *
	 * @param protocolVersion версия протокола Minecraft
	 * @param proxyHost       адрес прокси-сервера
	 * @param proxyPort       порт прокси-сервера
	 * @param type            тип прокси (SOCKS4 или SOCKS5)
	 * @return задержка в миллисекундах, или {@code -1} при ошибке
	 */
	private long pingThroughProxy(int protocolVersion, String proxyHost, int proxyPort, Proxy.Type type) {
		long start = System.currentTimeMillis();

		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(proxyHost, proxyPort), settings.maxDelayMs);
			socket.setSoTimeout(settings.maxDelayMs);

			var out = socket.getOutputStream();
			var in = socket.getInputStream();

			if (type == Proxy.Type.SOCKS5) {
				out.write(new byte[]{0x05, 0x01, 0x00});
				out.flush();

				byte[] greet = new byte[2];

				if (readFully(in, greet, 2) != 2 || greet[0] != 0x05) {
					return -1;
				}

				byte[] ip = InetAddress.getByName(settings.targetHost).getAddress();
				byte[] req = {
					0x05, 0x01, 0x00, 0x01,
					ip[0], ip[1], ip[2], ip[3],
					(byte) ((settings.targetPort >> 8) & 0xFF),
					(byte) (settings.targetPort & 0xFF)
				};
				out.write(req);
				out.flush();

				byte[] reply = new byte[4];

				if (readFully(in, reply, 4) < 4 || reply[0] != 0x05 || reply[1] != 0x00) {
					return -1;
				}

				drainSocks5ConnectReply(in, reply[3]);
			}
			else {
				byte[] ip = InetAddress.getByName(settings.targetHost).getAddress();
				byte[] req = {
					0x04, 0x01,
					(byte) ((settings.targetPort >> 8) & 0xFF), (byte) (settings.targetPort & 0xFF),
					ip[0], ip[1], ip[2], ip[3],
					0x00
				};
				out.write(req);
				out.flush();

				byte[] reply = new byte[8];

				if (readFully(in, reply, 8) < 2 || reply[0] != 0x00 || reply[1] != 0x5A) {
					return -1;
				}
			}

			sendMcHandshakePing(out, protocolVersion, settings.targetHost, settings.targetPort);

			if (in.read() >= 0) {
				return System.currentTimeMillis() - start;
			}
		}
		catch (IOException ignored) {}

		return -1;
	}

	private static void sendMcHandshakePing(OutputStream out, int protocolVersion, String host, int port)
			throws IOException {
		var payload = new ByteArrayOutputStream();
		writeVarInt(payload, protocolVersion);
		byte[] hostBytes = host.getBytes(StandardCharsets.UTF_8);
		writeVarInt(payload, hostBytes.length);
		payload.write(hostBytes);
		payload.write((port >> 8) & 0xFF);
		payload.write(port & 0xFF);
		writeVarInt(payload, ConnectionIntent.STATUS.getId());

		byte[] data = payload.toByteArray();
		var frame = new ByteArrayOutputStream();
		writeVarInt(frame, 1 + data.length);
		frame.write(0x00);
		frame.write(data);

		out.write(frame.toByteArray());
		out.write(new byte[]{0x01, 0x00});
		out.flush();
	}

	/**
	 * Записывает целое число в формате VarInt (переменная длина, до 5 байт) в поток.
	 *
	 * @param stream поток вывода
	 * @param value  значение для записи
	 * @throws IOException при ошибке записи в поток
	 */
	public static void writeVarInt(OutputStream stream, int value) throws IOException {
		while (true) {
			if ((value & ~SEGMENT_BITS) == 0) {
				stream.write(value);
				return;
			}

			stream.write((value & SEGMENT_BITS) | CONTINUE_BIT);
			value >>>= 7;
		}
	}

	/**
	 * Читает ровно {@code n} байт из потока в буфер, блокируясь до получения всех данных или EOF.
	 *
	 * @param in  входной поток
	 * @param buf буфер для записи
	 * @param n   количество байт для чтения
	 * @return фактически прочитанное количество байт (может быть меньше {@code n} при EOF)
	 * @throws IOException при ошибке чтения
	 */
	private static int readFully(InputStream in, byte[] buf, int n) throws IOException {
		int read = 0;

		while (read < n) {
			int r = in.read(buf, read, n - read);

			if (r < 0) {
				break;
			}

			read += r;
		}

		return read;
	}

	private static void drainSocks5ConnectReply(InputStream in, byte atyp) throws IOException {
		int skip = switch (atyp) {
			case 0x01 -> 4 + 2;
			case 0x04 -> 16 + 2;
			case 0x03 -> {
				int len = in.read();
				yield len + 2;
			}
			default -> 0;
		};

		if (skip > 0) {
			in.skipNBytes(skip);
		}
	}

	private static void closeQuietly(Socket s) {
		try {
			s.close();
		}
		catch (IOException ignored) {}
	}
}
