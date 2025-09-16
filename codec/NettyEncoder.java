@Sharable
public class NettyEncoder extends MessageToByteEncoder<Command> {

    /**
     * encode
     *
     * @param ctx channel handler context
     * @param msg command
     * @param out byte buffer
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Command msg, ByteBuf out) throws Exception {
        if (msg == null) {
            throw new RemotingException("encode msg is null");
        }
        out.writeByte(Command.MAGIC);
        out.writeByte(Command.VERSION);
        out.writeByte(msg.getType().ordinal());
        out.writeLong(msg.getOpaque());
        writeContext(msg, out);
        out.writeInt(msg.getBody().length);
        out.writeBytes(msg.getBody());
    }

    private void writeContext(Command msg, ByteBuf out) {
        byte[] headerBytes = msg.getContext().toBytes();
        out.writeInt(headerBytes.length);
        out.writeBytes(headerBytes);
    }
}
