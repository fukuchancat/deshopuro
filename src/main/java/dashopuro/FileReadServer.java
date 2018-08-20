package dashopuro;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileReadServer implements AutoCloseable {
	private AsynchronousChannelGroup group;
	private AsynchronousServerSocketChannel server;

	public FileReadServer(InetSocketAddress address) throws IOException {
		group = AsynchronousChannelGroup.withThreadPool(Executors.newWorkStealingPool());
		server = AsynchronousServerSocketChannel.open(group).bind(address);

		server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			@Override
			public void completed(AsynchronousSocketChannel worker, Void arg1) {
				try {
					server.accept(null, this);
					accepted(worker);
					worker.close();
				} catch (IOException | InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void failed(Throwable throwable, Void arg1) {
				throwable.printStackTrace();
			}
		});
	}

	private void accepted(AsynchronousSocketChannel worker)
			throws IOException, InterruptedException, ExecutionException {
		var stringBuffer = new StringBuffer();
		var byteBuffer = ByteBuffer.allocate(1024);

		if (worker.read(byteBuffer).get() > -1) {
			byteBuffer.flip();
			stringBuffer.append(Charset.defaultCharset().decode(byteBuffer).toString());
			byteBuffer.clear();
		}

		var path = Paths.get(stringBuffer.toString());
		var writeBytes = Files.readAllBytes(path);
		worker.write(ByteBuffer.wrap(writeBytes)).get();

		worker.close();
	}

	@Override
	public void close() throws IOException {
		server.close();
		group.shutdown();
	}
}
