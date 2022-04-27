package cx.rain.silk.patcher;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

import org.apache.commons.lang3.Validate;

public class ClassCache {
	private final byte[] hash;
	private final Map<String, byte[]> classes = new HashMap<>();
	private boolean converted;

	public ClassCache(byte[] hash) {
		this.hash = hash;
	}

	public void addClass(String name, byte[] bytes) {
		if (classes.containsKey(name)) {
			throw new IllegalArgumentException(name + " is already in ClassCache");
		}

		classes.put(name, Validate.notNull(bytes, "Passed null bytes for %s", name));
	}

	public Set<String> getClasses() {
		return classes.keySet();
	}

	public byte[] getClass(String name) {
		return classes.get(name);
	}

	public byte[] popClass(String name) {
		return classes.remove(name);
	}

	public byte[] getHash() {
		return hash;
	}

	private long calculateCRC() {
		CRC32 crc = new CRC32();

		crc.update(hash);
		for (byte[] clazz : classes.values()) crc.update(clazz);

		return crc.getValue();
	}

	public static ClassCache read(File input) throws IOException {
		try (DataInputStream dis = new DataInputStream(new GZIPInputStream(new FileInputStream(input)))) {
			char formatRevision = dis.readChar(); //Check the format of the file
			if (formatRevision != 'D') return new ClassCache(null);

			long expectedCRC = dis.readLong();

			//Read the hash
			byte[] hash = new byte[dis.readInt()];
			dis.readFully(hash);
			ClassCache classCache = new ClassCache(hash);

			for (int i = 0, count = dis.readInt(); i < count; i++) {
				byte[] nameBytes = new byte[dis.readInt()];
				dis.readFully(nameBytes);
				String name = new String(nameBytes, StandardCharsets.UTF_8);

				byte[] bytes = new byte[dis.readInt()];
				dis.readFully(bytes);
				classCache.classes.put(name, bytes);
			}

			//Ensure the read contents matches up with what was expected
			if (classCache.calculateCRC() != expectedCRC) return new ClassCache(null);

			return classCache;
		} catch (ZipException e) {
			//InflaterInputStream can throw this when the data is corrupt
			return new ClassCache(null);
		}
	}

	public void save(File output) throws IOException {
		if (output.exists()) {
			output.delete();
		}

		try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(output)))) {
			dos.writeChar('D'); //Format version
			dos.writeLong(calculateCRC()); //Expected CRC to get from fully reading

			//Write the hash
			dos.writeInt(hash.length);
			dos.write(hash);

			//Write the number of classes
			dos.writeInt(classes.size());
			for (Entry<String, byte[]> clazz : classes.entrySet()) {
				String name = clazz.getKey();
				byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
				byte[] bytes = clazz.getValue();

				//Write the name
				dos.writeInt(nameBytes.length);
				dos.write(nameBytes);

				//Write the actual bytes
				dos.writeInt(bytes.length);
				dos.write(bytes);
			}
		}
	}

	public boolean isConverted() {
		return converted;
	}
}
