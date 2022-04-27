package me.modmuss50.optifabric.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class ASMUtils {
	public static ClassNode readClass(byte[] bytes) {
		return readClass(new ClassReader(Objects.requireNonNull(bytes, "Cannot read null class bytes")));
	}

	public static ClassNode readClass(File file) throws IOException {
		try (InputStream in = new FileInputStream(Objects.requireNonNull(file, "Cannot read null file"))) {
			return readClass(new ClassReader(in));
		}
	}

	public static ClassNode readClass(ZipFile jar, ZipEntry entry) throws IOException {
		try (InputStream in = jar.getInputStream(entry)) {
			return readClass(new ClassReader(Objects.requireNonNull(in, "Entry not present in jar")));
		}
	}

	private static ClassNode readClass(ClassReader reader) {
		ClassNode node = new ClassNode();
		reader.accept(node, ClassReader.SKIP_FRAMES);
		return node;
	}
}