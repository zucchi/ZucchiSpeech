/*
 * TTSHTTPClient.java
 *
 * This is a simple command-line java app that shows how to use the NMDP HTTP Client Interface for
 *	Text-to-Speech (TTS) requests using the POST method
 *
 * This basic java app will:
 *	1. Create an instance of an HttpClient to interact with our HTTP Client Interface for TTS
 *	2. Use some simple helper methods to setup the URI and HTTP POST parameters
 *	3. Execute the HTTP Request
 *	4. Process the HTTP Response, writing the generated audio to file
 *
 *	Output of progress of the request is logged to console
 *	Values to be passed to the HTTP Client Interface are simply hard-coded class members for demo purposes
 *
 * @copyright  Copyright (c) 2010 Nuance Communications, inc. (http://www.nuance.com)
 *
 * @Created	: April 28, 2011
 * @Author	: Peter Freshman
 */
package com.nuance.nmsp.httpclient.tts;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;


public class TTSHTTPClient {

	/*
	 **********************************************************************************************************
	 * Client Interface Parameters:
	 *
	 * appId: 		You received this by email when you registered
	 * appKey:	 	You received this as a 64-byte Hex array when you registered.
	 * 				If you provide us with your username, we can convert this to a 128-byte string for you.
	 * id: 			Device Id is any character string. Typically a mobile device Id, but for test purposes, use the default value
	 * voice:		Provide either a voice or a language to be used to generate the audio. If you provide both a voice and language, the
	 * ttsLang:		language value will be ignored.
	 *
	 *				Please refer to the FAQ document available at the Nuance Mobile Developer website for a detailed list
	 *				of available voices and languages (http://dragonmobile.nuancemobiledeveloper.com/faq.php)
	 *
	 * codec:		The desired audio format. The supported codecs are:
	 *
	 *					audio/x-wav;codec=pcm;bit=16;rate=8000
	 *					audio/x-wav;codec=pcm;bit=16;rate=11025
	 * 					audio/x-wav;codec=pcm;bit=16;rate=16000
	 *					audio/x-wav;codec=pcm;bit=16;rate=22000
	 *					speex_nb', 'audio/x-speex;rate=8000
	 *					speex_wb', 'audio/x-speex;rate=16000
	 *					audio/amr
	 *					audio/qcelp
 	 *					audio/evrc
 	 *
	 * text:		The text to be passed to the client interface and converted to audio.
	 *
	 *********************************************************************************************************
	 */
	//private final static String APP_ID = "Insert Your App Id";
	//private final static String APP_KEY = "Insert Your 128-Byte App Key";
	private String DEVICE_ID = "0000";
	private String VOICE = "Samantha";
	private String LANGUAGE = "en_US";
	private String CODEC = "audio/x-wav;codec=pcm;bit=16;rate=22000";	//MP3
	private String TEXT = "Hello World. This is a greeting from Nuance.";

	/*********************************************************************************************************
	 *
	 * HTTP Client Interface URI parameters
	 *
	 * PORT:		To access this interface, port 443 is required
	 * HOSTNAME:	DNS address is tts.nuancemobility.net
	 * TTS_CMD:		TTS Servlet Resource
	 *
	 *********************************************************************************************************
	 */
	private static short PORT = (short) 443;
	private static String HOSTNAME = "tts.nuancemobility.net";
	private static String TTS = "/NMDPTTSCmdServlet/tts";

	/*
	 * HttpClient member to handle the TTS request/response
	 */
	private HttpClient httpclient = null;

	/*
	 * This function will initialize httpclient, set some basic HTTP parameters (version, UTF),
	 *	and setup SSL settings for communication between the httpclient and our Nuance servers
	 */
	private HttpClient getHttpClient() throws NoSuchAlgorithmException, KeyManagementException
	{
		// Standard HTTP parameters
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUseExpectContinue(params, false);

		// Initialize the HTTP client
		httpclient = new DefaultHttpClient(params);

		// Initialize/setup SSL
		TrustManager easyTrustManager = new X509TrustManager() {
			@Override
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] arg0, String arg1)
			throws java.security.cert.CertificateException {
				// TODO Auto-generated method stub
			}

			@Override
			public void checkServerTrusted(
					java.security.cert.X509Certificate[] arg0, String arg1)
			throws java.security.cert.CertificateException {
				// TODO Auto-generated method stub
			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				// TODO Auto-generated method stub
				return null;
			}
		};

		SSLContext sslcontext = SSLContext.getInstance("TLS");
		sslcontext.init(null, new TrustManager[] { easyTrustManager }, null);
		SSLSocketFactory sf = new SSLSocketFactory(sslcontext);
		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		Scheme sch = new Scheme("https", sf, PORT);	// PORT = 443
		httpclient.getConnectionManager().getSchemeRegistry().register(sch);

		// Return the initialized instance of our httpclient
		return httpclient;
	}

	/*
	 * This is a simple helper function to setup the query parameters. We need the following:
	 *	1. appId
	 *	2. appKey
	 *	3. id
	 *	4. voice or ttsLang
	 *		NOTE: Provide either a value for voice or ttsLang. If you provide both, voice will be used.
	 *
	 *	Please refer to the FAQ document available at the Nuance Mobile Developer website for a detailed list
	 *	of available voices and languages (http://dragonmobile.nuancemobiledeveloper.com/faq.php)
	 *
	 *	If your query fails, please be sure to review carefully what you are passing in for these
	 *	name/value pairs. Misspelled names, and improper values are a VERY common mistake.
	 */
	private URI getURI() throws Exception
	{
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();

		qparams.add(new BasicNameValuePair("appId", APP_ID));
		qparams.add(new BasicNameValuePair("appKey", APP_KEY));
		qparams.add(new BasicNameValuePair("id",  DEVICE_ID));
		qparams.add(new BasicNameValuePair("voice", VOICE));
		//qparams.add(new BasicNameValuePair("ttsLang", LANGUAGE));

		URI uri = URIUtils.createURI("https", HOSTNAME, PORT, TTS, URLEncodedUtils.format(qparams, "UTF-8"), null);

		return uri;
	}

	/*
	 * This is a simpler helper function to setup the Header parameters
	 */
	private HttpPost getHeader(URI uri) throws UnsupportedEncodingException
	{
		HttpPost httppost = new HttpPost(uri);
		httppost.addHeader("Content-Type",  "text/plain");
		httppost.addHeader("Accept", CODEC);

		// We'll also set the content of the POST request now...
		HttpEntity entity = new StringEntity(TEXT, "utf-8");;
		httppost.setEntity(entity);

		return httppost;
	}

	/*
	 * This function will take the HTTP response and parse out header values, write the audio that's been returned
	 *	to filed, and log details to the console
	 */
	private void processResponse(HttpResponse response) throws IllegalStateException, IOException
	{
		HttpEntity resEntity = response.getEntity();

		System.out.println("----------------------------------------");
		System.out.println(response.getStatusLine());

		// The request failed. Check out the status line to see what the problem is.
		//	Typically an issue with one of the parameters passed in...
		if (resEntity == null)
			return;

		// Grab the date
		Header date = response.getFirstHeader("Date");
		if( date != null )
			System.out.println("Date: " + date.getValue());

		// ALWAYS grab the Nuance-generated session id. Makes it a WHOLE LOT EASIER for us to hunt down your issues in our logs
		Header sessionid = response.getFirstHeader("x-nuance-sessionid");
		if( sessionid != null )
			System.out.println("x-nuance-sessionid: " + sessionid.getValue());

		// Check to see if we have a 200 OK response. Otherwise, review the technical documentation to understand why you recieved
		//	the HTTP error code that came back
		String status = response.getStatusLine().toString();
		boolean okFound = ( status.indexOf("200 OK") > -1 );
		if( okFound )
		{
			System.out.println("Response content length: " + resEntity.getContentLength());
			System.out.println("Chunked?: " + resEntity.isChunked());
		}

		// Grab the returned audio (or error message) returned in the body of the response
		InputStream in = resEntity.getContent();
		byte[] buffer = new byte[1024 * 16];
		int len;

		// Open up a stream to write audio to file
		OutputStream fos = null;
		String file = null;
		if (okFound)	// We have audio
		{
			file = System.currentTimeMillis() + ".wav";
		}  else			// No audio...
		{
			file = "log-err-"+ System.currentTimeMillis() + ".htm";
		}

		// Attempt to write to file...
		try {
			fos = new FileOutputStream(file);

			while((len = in.read(buffer)) > 0){
				fos.write(buffer, 0 , len);
			}
		} catch (Exception e) {
			System.err.println("Failed to save file: " + e.getMessage());
			e.printStackTrace();
		}

		// Finish up...
		finally {
			if(fos != null)
				try {
					fos.close();
					System.out.println("Saved file: " + file);
				} catch (IOException e) {
				}
		}

		System.out.println("----------------------------------------");

		// And we're done.
		resEntity.consumeContent();
	}

	/*
	 * main()
	 *
	 * This basic java app will:
	 *	1. Create an instance of an HttpClient to interact with our HTTP Client Interface for TTS
	 *	2. Use some simple helper methods to setup the URI and HTTP POST parameters
	 *	3. Execute the HTTP Request
	 *	4. Process the HTTP Response, writing the generated audio to file
	 *
	 *	Output of progress of the request is logged to console
	 *	Values to be passed to the HTTP Client Interface are simply hard-coded class members for demo purposes
	 *
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception{

		// Standard HTTP parameters
		TTSHTTPClient ttsHttpClient = new TTSHTTPClient();

		try {
			HttpClient httpclient = ttsHttpClient.getHttpClient();
			URI uri = ttsHttpClient.getURI();
			HttpPost httppost = ttsHttpClient.getHeader(uri);

			System.out.println("executing request " + httppost.getRequestLine());

			HttpResponse response = httpclient.execute(httppost);

			ttsHttpClient.processResponse(response);
		} finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			if(ttsHttpClient != null && ttsHttpClient.httpclient != null)
				ttsHttpClient.httpclient.getConnectionManager().shutdown();
		}

	}
}
