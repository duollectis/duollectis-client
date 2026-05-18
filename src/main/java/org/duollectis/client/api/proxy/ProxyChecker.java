package org.duollectis.client.api.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchronous proxy checker using Java 21 virtual threads.
 * Each proxy is checked in three stages:
 * Stage 1 — TCP connect to proxy host:port          → fail: DEAD_IP
 * Stage 2 — SOCKS5 / SOCKS4 handshake               → fail: DEAD_SERVER
 * Stage 3 — MC handshake ping through the proxy      → fail: PING_ERROR, pass: OK
 * Stage 3 can be disabled via {@link #pingStage3(boolean)}.
 * When disabled, the stage-1+2 round-trip time is recorded as the proxy ping.
 * Settings are persisted to {@code <gameDir>/duollectis/checker_settings.json}.
 */
public class ProxyChecker {

	private static final int SEGMENT_BITS = 0x7F;
	private static final int CONTINUE_BIT = 0x80;

	// ── singleton ─────────────────────────────────────────────────────────────

	@Getter
	private static final ProxyChecker instance = new ProxyChecker();

	@Getter
	private Settings settings = new Settings();

	private ProxyChecker() {}

	// fluent setters
	public ProxyChecker maxPings(int v) {
		this.settings.maxPings = Math.max(1, v);
		return this;
	}

	public ProxyChecker maxDelayMs(int v) {
		this.settings.maxDelayMs = Math.max(100, v);
		return this;
	}

	public ProxyChecker parallelism(int v) {
		this.settings.parallelism = Math.max(1, v);
		return this;
	}

	public ProxyChecker pingStage3(boolean v) {
		this.settings.pingStage3 = v;
		return this;
	}

	public ProxyChecker target(String host, int port) {
		if (host != null && !host.isBlank()) {
			this.settings.targetHost = host.trim();
		}

		if (port > 0 && port <= 65535) {
			this.settings.targetPort = port;
		}

		return this;
	}

	// ── settings persistence ──────────────────────────────────────────────────

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
		/**
		 * When false, stage 3 is skipped; stage-1+2 time is used as ping.
		 */
		@Expose
		private boolean pingStage3 = true;
		@Expose
		private int maxPings = 3;
		@Expose
		private int maxDelayMs = 5000;
		@Expose
		private int parallelism = 64;
	}

	public void saveSettings() {
		File file = settingsFile();
		file.getParentFile().mkdirs();

		try (FileWriter w = new FileWriter(file)) {
			SETTINGS_GSON.toJson(settings, w);
		}
		catch (IOException ignored) {
		}
	}

	public void loadSettings() {
		File file = settingsFile();
		if (!file.exists())
			return;
		try (FileReader r = new FileReader(file)) {
			settings = SETTINGS_GSON.fromJson(r, Settings.class);
		}
		catch (IOException ignored) {
		}
	}

	private static File settingsFile() {
		return new File(
				FabricLoader.getInstance().getGameDir().toFile(),
				"duollectis/checker_settings.json"
		);
	}

	// ── runtime state ─────────────────────────────────────────────────────────

	@Getter
	private volatile boolean running = false;
	private final AtomicInteger checked = new AtomicInteger(0);
	private final AtomicInteger total = new AtomicInteger(0);
	private final AtomicBoolean cancelled = new AtomicBoolean(false);
	private ExecutorService executor;
	private Semaphore semaphore;

	// ── start / stop ──────────────────────────────────────────────────────────

	/**
	 * Start checking all UNKNOWN proxies in the collection.
	 */
	public synchronized void start(int protocolVersion, Collection<Proxy> proxies) {
		List<Proxy> pending = proxies
				.stream()
				.filter(p -> p.getStatus() == Proxy.Status.UNKNOWN)
				.toList();

		if (!pending.isEmpty()) {
			startInternal(protocolVersion, pending);
		}
	}

	/**
	 * Cancel all pending checks; proxies still CHECKING are reset to UNKNOWN.
	 */
	public synchronized void cancel() {
		cancelled.set(true);
		if (executor != null)
			executor.shutdownNow();
		running = false;
	}

	private void startInternal(int protocolVersion, Collection<Proxy> proxies) {
		if (running)
			return;
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
					if (proxy.getStatus() == Proxy.Status.CHECKING)
						proxy.setStatus(Proxy.Status.UNKNOWN);
				}
			});
		}

		// Watcher: waits for all tasks, then saves results and clears running flag.
		Thread.ofVirtual().name("proxy-checker-watcher").start(() -> {
			executor.shutdown();
			try {
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException ignored) {
			}
			running = false;
			ProxyManager pm = ProxyManager.getInstance();
			if (pm != null)
				pm.save();
		});
	}

	// ── check logic ───────────────────────────────────────────────────────────

	private void checkProxy(int protocolVersion, Proxy proxy) {
		if (cancelled.get()) {
			if (proxy.getStatus() == Proxy.Status.CHECKING)
				proxy.setStatus(Proxy.Status.UNKNOWN);
			checked.incrementAndGet();
			return;
		}

		proxy.setStatus(Proxy.Status.CHECKING);
		proxy.setPingCount(0);
		proxy.setLatency(-1);

		long stageStart = System.currentTimeMillis();

		// Stage 1: TCP connect
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

		// Stage 2: SOCKS handshake
		Proxy.Type detectedType = null;
		for (int attempt = 0; attempt < settings.maxPings && !cancelled.get(); attempt++) {
			try {
				detectedType = detectType(probeSocket, proxy.getAddress(), proxy.getPort());
			}
			catch (IOException ignored) {
			}

			if (detectedType != null) {
				break;
			}
		}

		closeQuietly(probeSocket);

		if (detectedType == null) {
			proxy.setStatus(cancelled.get() ? Proxy.Status.UNKNOWN : Proxy.Status.DEAD_SERVER);
			checked.incrementAndGet();
			return;
		}

		// Stage 3: MC handshake ping (optional)
		if (!settings.pingStage3) {
			proxy.setType(detectedType);
			proxy.setLatency(System.currentTimeMillis() - stageStart);
			proxy.setPingCount(1);
			proxy.setStatus(Proxy.Status.OK);
			checked.incrementAndGet();
			return;
		}

		int successCount = 0;
		long totalPing = 0;

		for (int attempt = 0; attempt < settings.maxPings && !cancelled.get(); attempt++) {
			long ping = pingThroughProxy(protocolVersion, proxy.getAddress(), proxy.getPort(), detectedType);
			if (ping >= 0) {
				successCount++;
				totalPing += ping;
			}
		}

		if (cancelled.get()) {
			proxy.setStatus(Proxy.Status.UNKNOWN);
		}
		else if (successCount > 0) {
			proxy.setType(detectedType);
			proxy.setLatency(totalPing / successCount);
			proxy.setPingCount(successCount);
			proxy.setStatus(Proxy.Status.OK);
		}
		else {
			proxy.setType(detectedType);
			proxy.setStatus(Proxy.Status.PING_ERROR);
		}

		checked.incrementAndGet();
	}

	// ── SOCKS detection ───────────────────────────────────────────────────────

	/**
	 * Tries SOCKS5 greeting on the already-connected socket.
	 * If the server doesn't respond with VER=5, opens a fresh socket and tries SOCKS4.
	 * Returns the detected type, or null if neither works.
	 */
	private Proxy.Type detectType(Socket socket, String host, int port) throws IOException {
		var out = socket.getOutputStream();
		var in = socket.getInputStream();

		// SOCKS5 greeting: VER=5, NMETHODS=1, METHOD=0 (no auth)
		out.write(new byte[]{0x05, 0x01, 0x00});
		out.flush();
		byte[] resp = new byte[2];
		if (readFully(in, resp, 2) == 2 && resp[0] == 0x05 && (resp[1] & 0xFF) != 0xFF)
			return Proxy.Type.SOCKS5;

		// SOCKS4: CONNECT to 1.1.1.1:80
		try (Socket s4 = new Socket()) {
			s4.connect(new InetSocketAddress(host, port), settings.maxDelayMs);
			s4.setSoTimeout(settings.maxDelayMs);
			byte[] req = {0x04, 0x01, 0x00, 0x50, 0x01, 0x01, 0x01, 0x01, 0x00};
			s4.getOutputStream().write(req);
			s4.getOutputStream().flush();
			byte[] r4 = new byte[8];
			int read = readFully(s4.getInputStream(), r4, 8);
			// SOCKS4 reply: VN=0x00, CD=0x5A (granted)
			if (read >= 2 && r4[0] == 0x00 && r4[1] == 0x5A)
				return Proxy.Type.SOCKS4;
		}
		return null;
	}

	// ── MC ping through proxy ─────────────────────────────────────────────────

	/**
	 * Tunnels through the proxy to {@code targetHost:targetPort} and sends a
	 * Minecraft 1.7+ Status handshake. Returns round-trip ms on success, -1 on failure.
	 */
	private long pingThroughProxy(int protocolVersion, String proxyHost, int proxyPort, Proxy.Type type) {
		long start = System.currentTimeMillis();

		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(proxyHost, proxyPort), settings.maxDelayMs);
			socket.setSoTimeout(settings.maxDelayMs);
			var out = socket.getOutputStream();
			var in = socket.getInputStream();

			if (type == Proxy.Type.SOCKS5) {
				// Greeting
				out.write(new byte[]{0x05, 0x01, 0x00});
				out.flush();
				byte[] greet = new byte[2];
				int gr = readFully(in, greet, 2);
				if (gr != 2 || greet[0] != 0x05)
					return -1;

				// CONNECT request (IPv4 form — resolve locally, like Netty does)
				byte[] ip = java.net.InetAddress.getByName(settings.targetHost).getAddress();
				byte[] req = {
						0x05, // VER
						0x01, // CMD = CONNECT
						0x00, // RSV
						0x01, // ATYP = IPv4
						ip[0], ip[1], ip[2], ip[3],
						(byte) ((settings.targetPort >> 8) & 0xFF),
						(byte) (settings.targetPort & 0xFF)
				};
				out.write(req);
				out.flush();

				// CONNECT reply: VER REP RSV ATYP [addr] [port]
				byte[] reply = new byte[4];
				int r = readFully(in, reply, 4);
				if (r < 4 || reply[0] != 0x05 || reply[1] != 0x00) {
					return -1;
				}
				drainSocks5ConnectReply(in, reply[3]);

			}
			else { // SOCKS4
				byte[] ip = java.net.InetAddress.getByName(settings.targetHost).getAddress();
				byte[] req = {
						0x04, 0x01,
						(byte) ((settings.targetPort >> 8) & 0xFF), (byte) (settings.targetPort & 0xFF),
						ip[0], ip[1], ip[2], ip[3],
						0x00
				};
				out.write(req);
				out.flush();
				byte[] reply = new byte[8];
				int r = readFully(in, reply, 8);
				if (r < 2 || reply[0] != 0x00 || reply[1] != 0x5A) {
					return -1;
				}
			}

			// MC handshake + status request
			sendMcHandshakePing(out, protocolVersion, settings.targetHost, settings.targetPort);

			// Read first byte (blocks until server replies or timeout)
			int b = in.read();
			if (b >= 0) {
				return System.currentTimeMillis() - start;
			}

		}
		catch (IOException ignored) {
		}

		return -1;
	}

	// ── MC protocol helpers ───────────────────────────────────────────────────

	/**
	 * Writes a MC 1.7+ Handshake packet (id=0x00, next state=1 status).
	 */
	private static void sendMcHandshakePing(
			OutputStream out,
			int protocolVersion,
			String host,
			int port
	) throws IOException {
		// ── build handshake payload manually (no MC runtime needed) ──────
		var payload = new java.io.ByteArrayOutputStream();
		writeVarInt(payload, protocolVersion); // protocol version
		byte[] hostBytes = host.getBytes(StandardCharsets.UTF_8);
		writeVarInt(payload, hostBytes.length); // host string length
		payload.write(hostBytes); // host string
		payload.write((port >> 8) & 0xFF); // port high byte
		payload.write(port & 0xFF); // port low byte
		writeVarInt(payload, ConnectionIntent.STATUS.getId()); // next state = STATUS

		// ── frame: [packet-length] [packet-id=0x00] [payload] ────────────
		byte[] data = payload.toByteArray();
		var frame = new java.io.ByteArrayOutputStream();
		writeVarInt(frame, 1 + data.length); // length = id byte + payload
		frame.write(0x00);                   // packet id = Handshake
		frame.write(data);

		out.write(frame.toByteArray());

		// ── Status Request packet: length=1, id=0x00 ─────────────────────
		out.write(new byte[]{0x01, 0x00});
		out.flush();
	}


	public static void writeVarInt(OutputStream stream, int value) throws IOException {
		while (true) {
			if ((value & ~SEGMENT_BITS) == 0) {
				stream.write(value);
				return;
			}

			stream.write((value & SEGMENT_BITS) | CONTINUE_BIT);

			// Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
			value >>>= 7;
		}
	}

	/**
	 * Reads exactly {@code n} bytes; returns how many were actually read.
	 */
	private static int readFully(java.io.InputStream in, byte[] buf, int n) throws IOException {
		int read = 0;
		while (read < n) {
			int r = in.read(buf, read, n - read);
			if (r < 0)
				break;
			read += r;
		}
		return read;
	}

	/**
	 * Drains the address portion of a SOCKS5 CONNECT reply after the first 4 bytes.
	 */
	private static void drainSocks5ConnectReply(java.io.InputStream in, byte atyp) throws IOException {
		int skip = switch (atyp) {
			case 0x01 -> 4 + 2;   // IPv4 + port
			case 0x04 -> 16 + 2;  // IPv6 + port
			case 0x03 -> {
				int len = in.read();
				yield len + 2;
			} // domain + port
			default -> 0;
		};
		if (skip > 0)
			in.skipNBytes(skip);
	}

	// ── misc ──────────────────────────────────────────────────────────────────

	private static void closeQuietly(Socket s) {
		try {
			s.close();
		}
		catch (IOException ignored) {
		}
	}

	public static void main(String[] args) throws IOException {
		ProxyChecker checker = ProxyChecker.getInstance();

		// ── Direct ping (no proxy) ────────────────────────────────────────────
		System.out.println("=== Direct ping ===");
		Socket socket = null;
		while (true) {
			socket = new Socket();
			try {
				System.out.println("Connecting directly to connect.2k2f.ru:25565 ...");
				socket.connect(new InetSocketAddress("connect.2k2f.ru", 25565), 3000);
				socket.setSoTimeout(5000);
				break;
			}
			catch (Exception e) {
				System.err.println("Connect failed: " + e.getMessage());
				closeQuietly(socket);
				//				break; // don't loop forever in test
			}
		}

		if (socket.isConnected()) {
			try (Socket s = socket) {
				var out = s.getOutputStream();
				var in = s.getInputStream();

				sendMcHandshakePing(out, 767, "connect.2k2f.ru", 25565);

				// First blocking read — waits until the server actually sends data
				int first = in.read();
				if (first < 0) {
					System.err.println("Server closed connection without response");
				}
				else {
					StringBuilder b = new StringBuilder();
					b.append((char) first);
					// Read the rest that has already arrived
					while (in.available() > 0) {
						b.append((char) in.read());
					}
					System.out.println("Direct response: " + b);
				}
			}
		}

		// ── Ping through SOCKS5 proxy ─────────────────────────────────────────
		System.out.println("\n=== Ping through proxy ===");
		long ping = checker.pingThroughProxy(767, "37.220.83.249", 1080, Proxy.Type.SOCKS5);
		System.out.println("pingThroughProxy result: " + (ping >= 0 ? ping + " ms" : "FAILED"));
	}
}
