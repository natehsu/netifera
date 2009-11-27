package com.netifera.platform.host.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IFileSystem {
	String getNameSeparator();
	
	File[] getRoots();
	File[] getDirectoryList(String directoryName) throws IOException;

	File stat(String fileName) throws IOException;

	boolean delete(String fileName) throws IOException;
	boolean deleteDirectory(String directoryName) throws IOException;
	
	File createDirectory(String directoryName) throws IOException;
	
	boolean rename(String oldName, String newName) throws IOException;

//	boolean truncate(String fileName) throws IOException;
//	boolean create(String fileName, int permissions) throws IOException;

	InputStream getInputStream(String fileName) throws IOException;
	OutputStream getOutputStream(String fileName) throws IOException;
	
	void disconnect() throws IOException;
}
