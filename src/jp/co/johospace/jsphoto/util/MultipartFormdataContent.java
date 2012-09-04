package jp.co.johospace.jsphoto.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.google.api.client.http.AbstractHttpContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpContent;
import com.google.common.base.Preconditions;

/**
 * RFC1867 multipart/form-data のコンテンツ実装
 */
public class MultipartFormdataContent extends AbstractHttpContent {

	private final Map<String, HttpContent> mParts = new HashMap<String, HttpContent>();
	private String mBoundary = generateBoundary();
	
	private static final byte[] CRLF =
			"\r\n".getBytes();
	
	private static final byte[] DQUOTE =
			"\"".getBytes();
	
	private static final byte[] TWO_DASHES =
			"--".getBytes();
	
	private static final byte[] CONTENT_DISPOSITION_FORMDATA_NAME =
			"Content-Disposition: form-data; name=".getBytes();
	
	private static final byte[] FILENAME =
			"; filename=".getBytes();
	
	private static final byte[] CONTENT_TYPE =
			"Content-Type: ".getBytes();
	
	private static final byte[] CONTENT_TRANSFER_ENCODING =
			"Content-Transfer-Encoding: ".getBytes();
	
	private static final byte[] BINARY =
			"binary".getBytes();
	
	public MultipartFormdataContent() {
		super();
	}
	
	@Override
	public String getType() {
		return "multipart/form-data; boundary=\"" + mBoundary + "\"";
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		byte[] boundaryBytes = mBoundary.getBytes();
		
		out.write(TWO_DASHES);
		out.write(boundaryBytes);
		
		for (String name : mParts.keySet()) {
			out.write(CRLF);
			
			HttpContent content = mParts.get(name);
			
			out.write(CONTENT_DISPOSITION_FORMDATA_NAME);
			out.write(DQUOTE);
			out.write(name.getBytes());
			out.write(DQUOTE);
			if (content instanceof FileContent) {
				FileContent file = (FileContent) content;
				out.write(FILENAME);
				out.write(DQUOTE);
				out.write(file.getFile().getName().getBytes());
				out.write(DQUOTE);
			}
			out.write(CRLF);
			
			String contentType = content.getType();
			if (contentType == null) {
				contentType = "application/octet-stream";
			}
			out.write(CONTENT_TYPE);
			out.write(contentType.getBytes());
			out.write(CRLF);
			
			out.write(CONTENT_TRANSFER_ENCODING);
			out.write(BINARY);
			out.write(CRLF);
			
			out.write(CRLF);
			content.writeTo(out);
			out.write(CRLF);
			
			out.write(TWO_DASHES);
			out.write(boundaryBytes);
		}
		out.write(TWO_DASHES);
		out.flush();
	}
	
	@Override
	protected long computeLength() throws IOException {
		long length = 0L;
		
		byte[] boundaryBytes = mBoundary.getBytes();
		
		length += TWO_DASHES.length + boundaryBytes.length;
		
		for (String name : mParts.keySet()) {
			HttpContent content = mParts.get(name);
			if (content.getLength() < 0) {
				return -1L;
			}
			
			length += CRLF.length;
			
			length += CONTENT_DISPOSITION_FORMDATA_NAME.length + DQUOTE.length + name.getBytes().length + DQUOTE.length;
			if (content instanceof FileContent) {
				FileContent file = (FileContent) content;
				length += FILENAME.length + DQUOTE.length + file.getFile().getName().getBytes().length + DQUOTE.length;
			}
			length += CRLF.length;
			
			String contentType = content.getType();
			if (contentType == null) {
				contentType = "application/octet-stream";
			}
			length += CONTENT_TYPE.length + contentType.getBytes().length + CRLF.length;
			
			length += CONTENT_TRANSFER_ENCODING.length + BINARY.length + CRLF.length;
			
			length += CRLF.length + content.getLength() + CRLF.length;
			
			length += TWO_DASHES.length + boundaryBytes.length;
		}
		
		length += TWO_DASHES.length;
		
		return length;
	}
	
	public String getBoundary() {
		return mBoundary;
	}
	
	public MultipartFormdataContent setBoundary(String boundary) {
		mBoundary = Preconditions.checkNotNull(boundary);
		return this;
	}
	
	public MultipartFormdataContent addContent(String name, HttpContent content) {
		mParts.put(name, content);
		return this;
	}
	
	
	private static final char[] BOUNDARY_CHARS =
			"-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	public static String generateBoundary() {
		Random rnd = new Random();
		final int length = 30 + rnd.nextInt(11);
		StringBuilder boundary = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			boundary.append(BOUNDARY_CHARS[rnd.nextInt(BOUNDARY_CHARS.length)]);
		}
		return boundary.toString();
	}

}
