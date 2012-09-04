package jp.co.johospace.jsphoto.util;


import java.io.File;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class MediaStoreOperation {
	private static final String tag = MediaStoreOperation.class.getSimpleName();
	
	public static void scanAndDeleteMediaStoreEntry(Context context, File from, File to, boolean wait){
		deleteMediaStoreEntry(context, from);
		MediaUtil.scanMedia(context, to, wait);
	}
	
	public static void deleteMediaStoreEntry(Context context, File file){
//		android.util.Log.d("MediaStoreOperation", file.getAbsolutePath());	/*$debug$*/
		ContentResolver resolver = context.getContentResolver();
		deleteImageMediaStore(resolver, file);
		deleteVideoMediaStore(resolver, file);
	}

	private static void deleteVideoMediaStore(ContentResolver resolver, File file){
		Cursor cursor = null;
		String path = file.getAbsolutePath();

		try{
			String[] proj = { MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA };
			
			String selection = MediaStore.Video.Media.DATA + " = ?";
			//if(file.isDirectory()){
				selection = MediaStore.Video.Media.DATA +  " LIKE ?";
				path += "%";
			//}
			
			cursor = resolver.query(
				MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 
				proj, 
				selection, 
				new String[]{ path }, 
				null
			);
			
			if(cursor.moveToFirst()){
				do{
					Uri toDelete = ContentUris.appendId(
						MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon(),
						cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media._ID))
					).build();
					
					File f = new File(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)));
					if(!f.exists())
						resolver.delete(toDelete, null, null);
				}while(cursor.moveToNext());
			}
		}finally{
			cursor.close();
		}
	}
	
	private static void deleteImageMediaStore(ContentResolver resolver, File file){
		Cursor cursor = null;
		String path = file.getAbsolutePath();

		try{
			String[] proj = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA };
			
			String selection = MediaStore.Images.Media.DATA + " = ?";
			//if(file.isDirectory()){
				selection = MediaStore.Images.Media.DATA +  " LIKE ?";
				path += "%";
			//}
			
			cursor = resolver.query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
				proj, 
				selection, 
				new String[]{ path }, 
				null
			);
			
			if(cursor.moveToFirst()){
				do{
					Uri toDelete = ContentUris.appendId(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon(),
						cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID))
					).build();
					
					File f = new File(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
					if(!f.exists())
						resolver.delete(toDelete, null, null);

//					android.util.Log.d(tag, cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));	/*$debug$*/
				}while(cursor.moveToNext());
			}
		}finally{
			cursor.close();
		}
	}
}
