package dashopuro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

import com.aparapi.Kernel;

import lombok.Getter;
import lombok.Setter;

/**
 * 試問5 単一のファイルを書き込む・読み込むメソッドを作る
 * 
 * @author fukuchan
 */
@Getter
@Setter
public class FileManagerGPU {
	private int bufferSize;

	public FileManagerGPU(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public String read(Path path) throws IOException, InterruptedException, ExecutionException {
		var stream = new ByteArrayOutputStream();
		try (var channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ)) {
			var byteBuffer = ByteBuffer.allocate(bufferSize);
			var result = 0;
			while ((result = channel.read(byteBuffer, stream.size()).get()) > -1) {
				var bytes = new byte[result];
				byteBuffer.flip();
				byteBuffer.get(bytes);
				stream.write(bytes);
				byteBuffer.clear();
			}
		}
		return stream.toString(Charset.defaultCharset());
	}

	public void write(Path path, String string) throws IOException, InterruptedException, ExecutionException {
		var codepoints = string.codePoints().toArray();
		var dst = new byte[codepoints.length * 2 + 2];

		// UTF-16LEのBOMを書き込む
		dst[0] = (byte) 0xff;
		dst[1] = (byte) 0xfe;

		Kernel kernel = new Kernel() {
			@Override
			public void run() {
				int i = getGlobalId();
				int j = 0;
				int k = i * 2 + 2;
				int codepoint = codepoints[i];

				while (j < 2) {
					dst[k] = (byte) (codepoint & 0xff);
					codepoint >>= 8;
					j++;
					k++;
				}
			}
		};
		kernel.execute(codepoints.length);
		kernel.dispose();

		try (var channel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			var byteBuffer = ByteBuffer.wrap(dst);
			channel.write(byteBuffer, 0).get();
		}
	}

	public static void main(String[] args) {
		var manager = new FileManagerGPU(1024);
		var readPath = Paths.get("./file/read/雲林院の菩提講にて、翁たちの出会い.txt");
		var writePath = Paths.get("./file/write/雲林院の菩提講にて、翁たちの出会い.txt");
		try {
			var string = manager.read(readPath);
			manager.write(writePath, string);
		} catch (IOException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
