package dashopuro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MorseWriter implements AutoCloseable {
	private static final Map<String, String> MORSE_MAP = new HashMap<>() {
		private static final long serialVersionUID = 5829123569770090233L;
		{
			put(" ", " ");
			put("a", "･-");
			put("b", "-･･･");
			put("c", "-･-･");
			put("d", "-･･");
			put("e", "･");
			put("f", "･･-･");
			put("g", "--･");
			put("h", "････");
			put("i", "･･");
			put("j", "･---");
			put("k", "-･-");
			put("l", "･-･･");
			put("m", "--");
			put("n", "-･");
			put("o", "---");
			put("p", "･--･");
			put("q", "--･-");
			put("r", "･-･");
			put("s", "･･･");
			put("t", "-");
			put("u", "･･-");
			put("v", "･･･-");
			put("w", "･--");
			put("x", "-･･-");
			put("y", "-･--");
			put("1", "･----");
			put("2", "･･---");
			put("3", "･･･--");
			put("4", "････-");
			put("5", "･････");
			put("6", "-････");
			put("7", "--･･･");
			put("8", "---･･");
			put("9", "----･");
			put("0", "-----");
			put(".", "･-･-･-");
			put(",", "--･･--");
			put("?", "･･--･･");
			put("-", "-････-");
			put("/", "-･･-･");
			put("@", "･--･-･");
		}
	};

	private ByteArrayOutputStream stream = new ByteArrayOutputStream();

	public MorseWriter(String string) throws IOException {
		for (var i : string.chars().toArray()) {
			var sign = MORSE_MAP.get(new String(Character.toChars(i)).toLowerCase());
			if (sign == null)
				continue;
			for (var j : sign.chars().toArray()) {
				if (j == '･')
					addDit();
				if (j == '-')
					addDah();
				if (j == ' ')
					addLongBlank();
				addShortBlank();
			}
			addMiddleBlank();
		}
	}

	public void write(Path path) throws IOException {
		var dst = stream.toByteArray();
		var format = new AudioFormat(8000f, 8, 1, true, false);
		var pipedInputStream = new PipedInputStream();
		var pipedOutputStream = new PipedOutputStream();
		var audioInputStream = new AudioInputStream(pipedInputStream, format, dst.length);

		pipedOutputStream.connect(pipedInputStream);

		var service = Executors.newWorkStealingPool();
		service.submit(new Runnable() {
			@Override
			public void run() {
				try {
					pipedOutputStream.write(dst);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, path.toFile());
		pipedOutputStream.close();
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}

	private void addDit() throws IOException {
		var bytes = new byte[600 * 1];
		IntStream.range(0, bytes.length).forEach(i -> {
			if (i % 20 < 10)
				bytes[i] = (byte) 2;
			else
				bytes[i] = (byte) -2;
		});
		stream.write(bytes);
	}

	private void addDah() throws IOException {
		var bytes = new byte[600 * 3];
		IntStream.range(0, bytes.length).forEach(i -> {
			if (i % 20 < 10)
				bytes[i] = (byte) 2;
			else
				bytes[i] = (byte) -2;
		});
		stream.write(bytes);
	}

	private void addShortBlank() throws IOException {
		var bytes = new byte[600 * 1];
		Arrays.fill(bytes, (byte) 0);
		stream.write(bytes);
	}

	private void addMiddleBlank() throws IOException {
		var bytes = new byte[600 * 2];
		Arrays.fill(bytes, (byte) 0);
		stream.write(bytes);
	}

	private void addLongBlank() throws IOException {
		var bytes = new byte[600 * 4];
		Arrays.fill(bytes, (byte) 0);
		stream.write(bytes);
	}

	public static void main(String[] args) {
		try (MorseWriter morseWriter = new MorseWriter("ago no pose")) {
			Path path = Paths.get("./file/temp/ago no pose.wav");
			morseWriter.write(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
