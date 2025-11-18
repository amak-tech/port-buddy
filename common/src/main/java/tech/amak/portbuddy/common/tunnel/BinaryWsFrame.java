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

    /**
     * Encodes the given connection ID and data into a {@link ByteBuffer} following a specific binary
     * frame format. The encoded frame contains the connection ID length, the UTF-8 encoded connection ID,
     * and a portion of the data starting at the specified offset and up to the specified length.
     *
     * @param connectionId the connection identifier to be encoded (expected to be non-null)
     * @param data         the raw payload data to be included in the frame (expected to be non-null)
     * @param offset       the starting position of the data array to be included
     * @param length       the number of bytes from the data array to be included
     * @return a {@link ByteBuffer} containing the encoded frame data
     */
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

    /**
     * Encodes the given connection ID and data into a byte array following a specific binary
     * frame format. The encoded frame includes the connection ID length, the UTF-8 encoded
     * connection ID, and a portion of the data starting at the specified offset and up to the
     * specified length.
     *
     * @param connectionId the connection identifier to be encoded (expected to be non-null)
     * @param data         the raw payload data to be included in the frame (expected to be non-null)
     * @param offset       the starting position of the data array to be included
     * @param length       the number of bytes from the data array to be included
     * @return a byte array containing the encoded frame data
     */
    public static byte[] encodeToArray(final String connectionId,
                                       final byte[] data,
                                       final int offset,
                                       final int length) {
        final var bb = encodeToByteBuffer(connectionId, data, offset, length);
        final var out = new byte[bb.remaining()];
        bb.get(out);
        return out;
    }

    /**
     * Decodes a binary frame from the provided {@link ByteBuffer} into a {@code Decoded} record object.
     * The frame is expected to have a specific format, starting with a 2-byte length
     * field indicating the UTF-8 encoded connection ID's length, followed by the connection ID bytes,
     * and ending with the remaining data bytes.
     *
     * @param buffer the {@link ByteBuffer} containing the binary frame data to decode;
     *               must have sufficient remaining bytes to represent a valid frame.
     * @return a {@code Decoded} object containing the connection ID and data extracted from the frame,
     *     or {@code null} if the buffer does not contain a valid or complete frame.
     */
    public static Decoded decode(final ByteBuffer buffer) {
        if (buffer.remaining() < 2) {
            return null;
        }
        final var length = Short.toUnsignedInt(buffer.getShort());
        if (buffer.remaining() < length) {
            return null;
        }
        final var idBytes = new byte[length];
        buffer.get(idBytes);
        final var connectionId = new String(idBytes, StandardCharsets.UTF_8);
        final var data = new byte[buffer.remaining()];
        buffer.get(data);
        return new Decoded(connectionId, data);
    }

    /**
     * Decodes a binary frame from the provided byte array into a {@code Decoded} record object.
     * The frame is expected to have a specific format, starting with a 2-byte length
     * field indicating the UTF-8 encoded connection ID's length, followed by the connection ID bytes,
     * and ending with the remaining data bytes.
     *
     * @param frameBytes the byte array containing the binary frame data to decode;
     *                   must have sufficient bytes to represent a valid frame.
     * @return a {@code Decoded} object containing the connection ID and data extracted from the frame,
     *     or {@code null} if the array does not contain a valid or complete frame.
     */
    public static Decoded decode(final byte[] frameBytes) {
        return decode(ByteBuffer.wrap(frameBytes));
    }

    /**
     * A record that represents the result of decoding a binary WebSocket frame.
     * It contains a connection identifier and the corresponding payload data.
     *
     * <ul>
     *   <li>The {@code connectionId} represents the unique identifier of the connection.
     *   <li>The {@code data} represents the raw payload data associated with the frame.
     * </ul>
     * Instances of this record are typically produced by decoding operations on binary
     * WebSocket frames, which follow a specific format.
     */
    public record Decoded(String connectionId, byte[] data) {
    }
}
