package tech.amak.portbuddy.common.tunnel;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Utility to encode/decode binary WebSocket frames for TCP tunneling.
 * Frame format (big-endian):
 * - 2 bytes: unsigned short representing the byte length of the UTF-8 encoded connectionId (N)
 * - N bytes: connectionId UTF-8 bytes
 * - R bytes: raw payload data
 */
public final class BinaryWsFrame {

    private BinaryWsFrame() {
    }

    public static ByteBuffer encodeToByteBuffer(final String connectionId,
                                                final byte[] data,
                                                final int offset,
                                                final int length) {
        final var idBytes = connectionId.getBytes(StandardCharsets.UTF_8);
        final var capacity = 2 + idBytes.length + length;
        final var buffer = ByteBuffer.allocate(capacity);
        buffer.putShort((short) (idBytes.length & 0xFFFF));
        buffer.put(idBytes);
        buffer.put(data, offset, length);
        buffer.flip();
        return buffer;
    }

    public static byte[] encodeToArray(final String connectionId, final byte[] data, final int offset,
                                       final int length) {
        final var bb = encodeToByteBuffer(connectionId, data, offset, length);
        final var out = new byte[bb.remaining()];
        bb.get(out);
        return out;
    }

    public static Decoded decode(final ByteBuffer buffer) {
        if (buffer.remaining() < 2) {
            return null;
        }
        final var len = Short.toUnsignedInt(buffer.getShort());
        if (buffer.remaining() < len) {
            return null;
        }
        final var idBytes = new byte[len];
        buffer.get(idBytes);
        final var connId = new String(idBytes, StandardCharsets.UTF_8);
        final var data = new byte[buffer.remaining()];
        buffer.get(data);
        return new Decoded(connId, data);
    }

    public static Decoded decode(final byte[] frameBytes) {
        return decode(ByteBuffer.wrap(frameBytes));
    }

    public record Decoded(String connectionId, byte[] data) {
    }
}
