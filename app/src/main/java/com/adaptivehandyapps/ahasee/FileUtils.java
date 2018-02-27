// Project: AHA PM Admin
// Contributor(s): M.A.Tucker
// Copyright 2014 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileUtils {

	private final static String TAG = "FileUtils";

	////////////////////////////////class data////////////////////////////////
	// standard storage locations
	public static final String APP_DIR = "/Android/data/com.adaptivehandyapps.ahasee/";
	public static final String DOWNLOAD_DIR = "/Download/";
	public static final String NULL_MARKER = "nada";

    public static final String FILE_EXTENSION_XML = "xml";

	///////////////////////////////////////////////////////////////////////////////
	// constructor
    public FileUtils() {
	}

	/////////////////////////////private interfaces/////////////////////////////
    // get or create target directory
	private static File getTargetStorageDir(String targetDir) {
//		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + APP_DIR + targetDir;
//		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + APP_DIR;
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + targetDir;
		Log.v(TAG, "getTargetStorageDir: " + path);
		return new File (path);
	}
	//////////////////////////////public interfaces////////////////////////////
	// get target directory
	public static File getTargetDir(String targetDir) {
		File storageDir = null;
		
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			storageDir = getTargetStorageDir(targetDir);
			Log.d(TAG, "getTargetDir: storageDir " + storageDir);
			
			if (storageDir != null) {
                if (!storageDir.exists()) {
                    // if directory does not exist, try making directory w/ parent directories
                    if (!storageDir.mkdirs()) {
                        Log.e(TAG, "getTargetDir unable to find or make data directory " + targetDir + ".");
                        return null;
                    }
				}
			}
		} else {
			Log.v(TAG, "getTargetDir: External storage not mounted R/W.");
		}
		return storageDir;
	}
	
    ///////////////////////////////////////////////////////////////////////////////
    // get folders
    public static List<String> getFoldersList(String targetFolder) {
        List<String> folderList = new ArrayList<String>();
        File folder = getTargetStorageDir(targetFolder);
        if (folder.isDirectory()) {

            File[] fileList = folder.listFiles();
            for (int j = 0; j < fileList.length; j++) {
                if (fileList[j].isDirectory()) {
                    folderList.add(fileList[j].getName());
                    Log.v(TAG, "Project folder(" + j + ") " + folderList.get(j));
                }
            }
        }
        else {
            // not a project folder!
			Log.e(TAG, "Invalid taregt folder: " + targetFolder);
        }
        return folderList;
    }
	///////////////////////////////////////////////////////////////////////////////
	// get list of files within folder - either full path or name
	public static List<String> getFilesList(String targetDir, boolean fullpath, String filterOnExtension) {
		// get list of images in specified dir
		List<String> fileList = new ArrayList<>();
		String filename;
		File folder;
		folder = getTargetDir(targetDir);

		Log.v(TAG, "targetDir: " + targetDir);
		if (folder.isDirectory()) {

			File[] fileHandles = folder.listFiles();
//			Arrays.sort(fileHandles, new Comparator<File>() {
//				public int compare(File f1, File f2) {
//					return Long.compare(f1.lastModified(), f2.lastModified());
//				}
//			});
			for (int j = 0; j < fileHandles.length; j++) {
				if (fileHandles[j].isFile()) {
					if (fullpath) {
						// if full path requested, get path
						filename = fileHandles[j].getPath();
					}
					else {
						// full path not requested, get name
						filename = fileHandles[j].getName();
					}
                    int dot = filename.lastIndexOf(".");
                    String ext = filename.substring(dot + 1);
//					Log.v(TAG, "File(" + j + ") " + filename + ", ext " + ext);
					if(filterOnExtension.isEmpty() || (!filterOnExtension.isEmpty() && ext.toLowerCase().equals(filterOnExtension))) {
                        fileList.add(filename);
                        Log.v(TAG, "File(" + j + ") " + filename + " added.");
                    }
				}
			}
			Collections.sort(fileList);

		} else {
			// not an directory!
			Log.e(TAG, "Invalid directory name: " + APP_DIR + targetDir);
		}
		return fileList;
	}

    ///////////////////////////////////////////////////////////////////////////////
    // get input stream
    public static BufferedInputStream getFeed(String targetPath) {
        Log.v(TAG, "getFeed for: " + targetPath);
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(targetPath));
        }
        catch (Exception ex) {
            Log.e(TAG, "Exception: " + ex.getMessage());
        }
        return bis;
    }

    ///////////////////////////////////////////////////////////////////////////////
	// read feed
 	public static String readFeed(String targetPath) {
 		String feed = "";
 		try {
	 		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(targetPath));
	 		byte[] readBuffer = new byte[1024];
	
	 		int bytesRead=0;
	 		while( (bytesRead = bis.read(readBuffer)) != -1){ 
	 			feed = feed.concat(new String(readBuffer, 0, bytesRead));
//		 		System.out.print("readBuffer: " + feed);
	 		}
 		}
 		catch (Exception ex) {
			Log.e(TAG, "Exception: " + ex.getMessage());
			return NULL_MARKER;
 		}
 		Log.v(TAG, "Feed length: " + feed.length());
 		return feed;
 	}

	///////////////////////////////////////////////////////////////////////////////
}
