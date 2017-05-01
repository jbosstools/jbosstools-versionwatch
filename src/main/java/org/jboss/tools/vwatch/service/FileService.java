package org.jboss.tools.vwatch.service;

import java.io.*;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class FileService {
		
	private static FileService instance = null;;
	
	private FileService() {};
	
	public static  FileService getInstance() {
		if (instance == null) instance = new FileService();
		return instance;
		
	}
	
	public long getFolderSize(File dir) {
		long size = 0;
		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				System.out.println(file.getName() + " " + file.length());
				size += file.length();
			} else
				size += getFolderSize(file);
		}
		return size;
	}
	
	
	public void zipFolder(File sourceFolder,File destinationFile) {
		ZipFile bundleJar = null;
		try {
			bundleJar = new ZipFile(destinationFile);
			ZipParameters parameters = new ZipParameters();
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			parameters
					.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
			bundleJar.createZipFileFromFolder(sourceFolder,
					parameters, false, 10485760);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String exportResource(String resourceName) throws Exception {
		InputStream stream = null;
		OutputStream resStreamOut = null;
		String jarFolder;
		try {
			stream = FileService.class.getResourceAsStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
			if(stream == null) {
				throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
			}

			int readBytes;
			byte[] buffer = new byte[4096];
			jarFolder = new File(FileService.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath().replace('\\', '/');
			resStreamOut = new FileOutputStream(jarFolder + resourceName);
			while ((readBytes = stream.read(buffer)) > 0) {
				resStreamOut.write(buffer, 0, readBytes);
			}
		} catch (Exception ex) {
			throw ex;
		} finally {
			stream.close();
			resStreamOut.close();
		}

		return jarFolder + resourceName;
	}

	public String getRelPath(String base, String path) {
		String relative = new File(base).toURI().relativize(new File(path).toURI()).getPath();
		return relative;
	}
}

