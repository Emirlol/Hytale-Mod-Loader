package me.ancientri.hytale.modloader;

import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class HytaleMetadataLookup {
	private String name = "Hytale";
	private String title;
	private String branch;
	private String version;
	private String revisionId;

	public HytaleMetadataLookup(Path gameJar) {
		try (ZipFile zipFile = new ZipFile(gameJar.toFile())) {
			ZipEntry entry = zipFile.getEntry("META-INF/MANIFEST.MF");
			if (entry == null) throw new IOException("Game jar does not contain `META-INF/MANIFEST.MF`!");
			/*
			Example manifest contents:
Implementation-Branch: release
Implementation-Build: NoJar
Implementation-Patchline: release
Implementation-Revision-Id: dcad8778f19e4e56af55d74b58575c91c50a018d
Implementation-Title: Server
Implementation-Vendor-Id: com.hypixel.hytale
Implementation-Version: 2026.01.13-dcad8778f
			 */

			try (InputStream inputStream = zipFile.getInputStream(entry)) {
				Manifest manifest = new Manifest(inputStream);
				Attributes attributes = manifest.getMainAttributes();
				this.title = attributes.getValue("Implementation-Title");
				this.branch = attributes.getValue("Implementation-Branch");
				this.version = attributes.getValue("Implementation-Version");
				this.revisionId = attributes.getValue("Implementation-Revision-Id"); // The first 9 characters of this are appended to the end of the version
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (this.title == null || this.version == null || this.branch == null || this.revisionId == null) {
			Log.error(LogCategory.GAME_PROVIDER, "Incomplete game metadata retrieved from manifest:");
			Log.error(LogCategory.GAME_PROVIDER, "  Title: " + this.title);
			Log.error(LogCategory.GAME_PROVIDER, "  Version: " + this.version);
			Log.error(LogCategory.GAME_PROVIDER, "  Branch: " + this.branch);
			Log.error(LogCategory.GAME_PROVIDER, "  Revision ID: " + this.revisionId);
			throw new IllegalStateException("Failed to retrieve game metadata");
		}
	}

	public String getName() {
		return name + " " + title;
	}

	public String getVersion() {
		return version;
	}

	public String getBranch() {
		return branch;
	}

	public String getRevisionId() {
		return revisionId;
	}
}
