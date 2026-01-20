package com.infraly.sourcequery.protocol;

import com.hypixel.hytale.common.util.java.ManifestUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ProtocolSettings;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Options;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.Universe;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import static com.infraly.sourcequery.protocol.A2SProtocol.*;

public final class A2SPacketHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final int MIN_PACKET_SIZE = 5;
    private static final byte PROTOCOL_VERSION = 0x11;
    private static final byte ENVIRONMENT = resolveEnvironment();

    private final HytaleLogger logger;
    private final int gamePort;

    public A2SPacketHandler(HytaleLogger logger) {
        this.logger = logger;
        this.gamePort = Options.getOptionSet().valuesOf(Options.BIND).getFirst().getPort();
    }

    private static byte resolveEnvironment() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return ENVIRONMENT_WINDOWS;
        if (os.contains("mac")) return ENVIRONMENT_MAC;
        return ENVIRONMENT_LINUX;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        ByteBuf data = packet.content();
        if (data.readableBytes() < MIN_PACKET_SIZE) return;
        if (data.readIntLE() != PACKET_HEADER) return;

        ByteBuf response = handleRequest(data.readByte(), data, packet.sender());
        if (response != null) {
            ctx.writeAndFlush(new DatagramPacket(response, packet.sender()));
        }
    }

    private ByteBuf handleRequest(byte type, ByteBuf data, InetSocketAddress sender) {
        try {
            return switch (type) {
                case REQUEST_INFO -> buildInfoResponse();
                case REQUEST_PLAYER -> buildPlayerResponse(data, sender);
                case REQUEST_RULES -> buildRulesResponse(data, sender);
                case REQUEST_CHALLENGE -> buildChallengeResponse(sender);
                default -> null;
            };
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Error handling A2S request: %s", e.getMessage());
            return null;
        }
    }

    private ByteBuf buildChallengeResponse(InetSocketAddress sender) {
        ByteBuf buf = Unpooled.buffer(9);
        buf.writeIntLE(PACKET_HEADER);
        buf.writeByte(RESPONSE_CHALLENGE);
        buf.writeIntLE(ChallengeManager.generate(sender));
        return buf;
    }

    private ByteBuf buildInfoResponse() {
        ByteBuf buf = Unpooled.buffer(256);
        buf.writeIntLE(PACKET_HEADER);
        buf.writeByte(RESPONSE_INFO);
        buf.writeByte(PROTOCOL_VERSION);

        writeString(buf, HytaleServer.get().getServerName());

        var world = Universe.get().getDefaultWorld();
        writeString(buf, world != null ? world.getName() : "unknown");
        writeString(buf, "hytale");
        writeString(buf, "Hytale");

        buf.writeShortLE(0);
        buf.writeByte(Universe.get().getPlayerCount());
        buf.writeByte(HytaleServer.get().getConfig().getMaxPlayers());
        buf.writeByte(0);
        buf.writeByte(SERVER_TYPE_DEDICATED);
        buf.writeByte(ENVIRONMENT);
        buf.writeByte(0);
        buf.writeByte(VAC_UNSECURED);

        writeString(buf, ManifestUtil.getImplementationVersion());
        buf.writeByte(GAMEPORT_FLAG);
        buf.writeShortLE(gamePort);

        return buf;
    }

    private ByteBuf buildPlayerResponse(ByteBuf request, InetSocketAddress sender) {
        if (request.readableBytes() < 4) return null;

        int challenge = request.readIntLE();
        if (!ChallengeManager.validate(sender, challenge)) {
            return buildChallengeResponse(sender);
        }

        ByteBuf buf = Unpooled.buffer(128);
        buf.writeIntLE(PACKET_HEADER);
        buf.writeByte(RESPONSE_PLAYER);

        int countIndex = buf.writerIndex();
        buf.writeByte(0);

        int count = 0;
        for (var world : Universe.get().getWorlds().values()) {
            for (var player : world.getPlayerRefs()) {
                buf.writeByte(count++);
                writeString(buf, player.getUsername());
                buf.writeIntLE(0);
                buf.writeFloatLE(0f);
            }
        }
        buf.setByte(countIndex, count);

        return buf;
    }

    private ByteBuf buildRulesResponse(ByteBuf request, InetSocketAddress sender) {
        if (request.readableBytes() < 4) return null;

        int challenge = request.readIntLE();
        if (!ChallengeManager.validate(sender, challenge)) {
            return buildChallengeResponse(sender);
        }

        ByteBuf buf = Unpooled.buffer(512);
        buf.writeIntLE(PACKET_HEADER);
        buf.writeByte(RESPONSE_RULES);

        int countIndex = buf.writerIndex();
        buf.writeShortLE(0);

        int count = 0;
        count += writeRule(buf, "version", ManifestUtil.getImplementationVersion());
        count += writeRule(buf, "revision", ManifestUtil.getImplementationRevisionId());
        count += writeRule(buf, "patchline", ManifestUtil.getPatchline());
        count += writeRule(buf, "protocol_version", String.valueOf(ProtocolSettings.PROTOCOL_VERSION));
        count += writeRule(buf, "protocol_hash", ProtocolSettings.PROTOCOL_HASH);

        var world = Universe.get().getDefaultWorld();
        if (world != null) {
            count += writeRule(buf, "default_world", world.getName());
            count += writeRule(buf, "default_world_tps", String.valueOf(world.getTps()));
        }
        count += writeRule(buf, "world_count", String.valueOf(Universe.get().getWorlds().size()));
        count += writeRule(buf, "max_view_radius", String.valueOf(HytaleServer.get().getConfig().getMaxViewRadius()));

        long enabledPlugins = PluginManager.get().getPlugins().stream().filter(p -> p.isEnabled()).count();
        count += writeRule(buf, "loaded_plugins", String.valueOf(enabledPlugins));

        buf.setShortLE(countIndex, count);
        return buf;
    }

    private int writeRule(ByteBuf buf, String name, String value) {
        writeString(buf, name);
        writeString(buf, value != null ? value : "");
        return 1;
    }

    private void writeString(ByteBuf buf, String str) {
        if (str != null) {
            buf.writeCharSequence(str, StandardCharsets.UTF_8);
        }
        buf.writeByte(0);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.at(Level.WARNING).log("A2S handler exception: %s", cause.getMessage());
    }
}
