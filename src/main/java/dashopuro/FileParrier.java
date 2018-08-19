package dashopuro;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

/**
 * 試問4 あるディレクトリ以下の全テキストファイルを読み込み、内容をコンソールと他ファイルに出力する
 * 
 * @author fukuchan
 */
@Getter
@Setter
public class FileParrier {
	private Path readPath;
	private Path writePath;

	public FileParrier(Path readPath, Path writePath) {
		this.readPath = readPath;
		this.writePath = writePath;
	}

	public byte[] readBytes(InetSocketAddress address) throws IOException, InterruptedException, ExecutionException {
		try (var client = AsynchronousSocketChannel.open()) {
			var byteBuffer = ByteBuffer.allocate((int) Files.size(readPath));
			client.connect(address).get();
			client.write(ByteBuffer.wrap(readPath.toString().getBytes())).get();
			client.read(byteBuffer).get();

			var bytes = byteBuffer.array();
			System.out.println(new String(bytes));

			return bytes;
		}
	}

	public void writeBytes(byte[] bytes) throws IOException {
		Files.write(writePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	public void parry(InetSocketAddress address) throws IOException, InterruptedException, ExecutionException {
		var bytes = readBytes(address);
		writeBytes(bytes);
	}

	public static void main(String[] args) {
		var address = new InetSocketAddress("localhost", 927);
		var readDirectory = Paths.get("./file/read");
		var writeDirectory = Paths.get("./file/write");

		try (var server = new FileReadServer(address)) {
			var paths = Files.list(readDirectory).collect(Collectors.toList());
			for (var readPath : paths) {
				var writePath = writeDirectory.resolve(readPath.getFileName());
				var parrier = new FileParrier(readPath, writePath);
				parrier.parry(address);
			}
		} catch (IOException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
