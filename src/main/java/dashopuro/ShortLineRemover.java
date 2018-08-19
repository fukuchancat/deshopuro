package dashopuro;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 試問6 ファイル出力時に行が指定以上の大きさの行のみ出力する。
 * 
 * @author fukuchan
 */
public class ShortLineRemover {
	private int threshold;

	public ShortLineRemover(int threshold) {
		this.threshold = threshold;
	}

	public void removeLine(Path readPath, Path tempPath, Path writePath) throws IOException {
		var printWriter = new PrintWriter(Files.newBufferedWriter(writePath));
		var lines = Files.readAllLines(readPath);
		for (var i = 0; i < lines.size(); i++) {
			var line = lines.get(i);
			var morseWriter = new MorseWriter(line);
			var builder = new StringBuilder(tempPath.toString());
			builder.insert(builder.lastIndexOf("."), "-" + i);

			var path = Paths.get(builder.toString());
			morseWriter.write(path);

			if (Files.size(path) > threshold)
				printWriter.println(line);

			morseWriter.close();
		}
		printWriter.close();
	}

	public static void main(String[] args) {
		var readPath = Paths.get("./file/read/Capulet's orchard.txt");
		var tempPath = Paths.get("./file/temp/Capulet's orchard.wav");
		var writePath = Paths.get("./file/write/Capulet's orchard.txt");

		var remover = new ShortLineRemover(1024);
		try {
			remover.removeLine(readPath, tempPath, writePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
