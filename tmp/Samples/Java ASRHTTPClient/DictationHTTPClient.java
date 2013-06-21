/*
 * DictationHTTPClient.java
 *
 * This is a simple command-line java app that shows how to use the NMDP HTTP Client Interface for
 *	Dictation and WebSearch requests using the POST method
 *
 * This basic java app will:
 *	1. Create an instance of an HttpClient to interact with our HTTP Client Interface for TTS
 *	2. Use some simple helper methods to setup the URI and HTTP POST parameters
 *	3. Execute the HTTP Request, passing streamed audio from file to the interface
 *	4. Process the HTTP Response, writing the results to the console
 *
 *	Output of progress of the request is logged to console
 *	Values to be passed to the HTTP Client Interface are simply hard-coded class members for demo purposes
 *
 * @copyright  Copyright (c) 2010 Nuance Communications, inc. (http://www.nuance.com)
 *
 * @Created	: June 6, 2011
 * @Author	: Peter Freshman
 */
package com.nuance.nmsp.httpclient.dictation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.sf.json.JSONObject;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import util.streamer.FileAudioStreamer;

public class DictationHTTPClient {

	/*
	 **********************************************************************************************************
	 * Client Interface Parameters:
	 *
	 * appId: 		You received this by email when you registered
	 * appKey:	 	You received this as a 64-byte Hex array when you registered.
	 * 				If you provide us with your username, we can convert this to a 128-byte string for you.
	 * id: 			Device Id is any character string. Typically a mobile device Id, but for test purposes, use the default value
	 * Language:	The language code to use.
	 *
	 *				Please refer to the FAQ document available at the Nuance Mobile Developer website for a detailed list
	 *				of available languages (http://dragonmobile.nuancemobiledeveloper.com/faq.php)
	 *
	 * codec:		The desired audio format. The supported codecs are:
	 *
	 *					audio/x-wav;codec=pcm;bit=16;rate=8000
	 *					audio/x-wav;codec=pcm;bit=16;rate=11025
	 * 					audio/x-wav;codec=pcm;bit=16;rate=16000
	 *					audio/x-wav
	 *					speex_nb', 'audio/x-speex;rate=8000
	 *					speex_wb', 'audio/x-speex;rate=16000
	 *					audio/amr
	 *					audio/qcelp
 	 *					audio/evrc
 	 *
	 * Language Model:	The language model to be used for speech-to-text conversion. Supported values are
	 * 					Dictation and WebSearch
	 * 
	 * Results Format: The format the results she be returned as. Supported values are text/plan and application/xml.
	 * 					Currently, application/xml is ignored and will return results as text/plain. However, the next
	 * 					release of Network Speech Services will support results returned in xml format.
	 *
	 *********************************************************************************************************
	 */
	private String APP_ID = "Insert Your App Id";
	private String APP_KEY = "Insert Your 128-Byte App Key";
	private String DEVICE_ID = "0000";
	private String LANGUAGE = "en_US";
	private String CODEC = "audio/x-wav;codec=pcm;bit=16;rate=16000";	//MP3
	private String LM = "Dictation";	// or WebSearch
	private String RESULTS_FORMAT = "text/plain";	// or application/xml
	

	/*********************************************************************************************************
	 *
	 * HTTP Client Interface URI parameters
	 *
	 * PORT:		To access this interface, port 443 is required
	 * HOSTNAME:	DNS address is dictation.nuancemobility.net
	 * SERVLET:		Dictation Servlet Resource
	 *
	 *********************************************************************************************************
	 */
	private static short PORT = (short) 443;
	private static String HOSTNAME = "dictation.nuancemobility.net";
	private static String SERVLET = "/NMDPAsrCmdServlet/dictation";

	private static String ADD_CONTEXT = "/NMDPAsrCmdServlet/addContext";
	
	private static final String SAMPLE_RATE_8K  = "8K";
	private static final String SAMPLE_RATE_11K = "11K";
	private static final String SAMPLE_RATE_16K = "16K";

	private static String cookie = null;

	/*
	 * HttpClient member to handle the Dictation request/response
	 */
	private HttpClient httpclient = null;
	
	private boolean encodeToSpeex = false;

	private String sampleRate = "16000";
	private boolean isStreamed = false;
	private long fileSize = 0L;
	private String audioFile = "/tmp/audio_16k16bit.pcm";

	/*
	 * This function will initialize httpclient, set some basic HTTP parameters (version, UTF),
	 *	and setup SSL settings for communication between the httpclient and our Nuance servers
	 */
	@SuppressWarnings("deprecation")
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
	 *
	 *	If your query fails, please be sure to review carefully what you are passing in for these
	 *	name/value pairs. Misspelled names, and invalid AppKey values are a VERY common mistake.
	 */
	private List<NameValuePair> setParams()
	{
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();

		qparams.add(new BasicNameValuePair("appId", APP_ID));
		qparams.add(new BasicNameValuePair("appKey", APP_KEY));
		qparams.add(new BasicNameValuePair("id",  DEVICE_ID));

		return qparams;
	}

	/*
	 * This is a simple helper function to create the URI.
	 */
	private URI getURI() throws Exception
	{
		// Get the standard set of parameters to be passed in...
		List<NameValuePair> qparams = this.setParams();

		URI uri = URIUtils.createURI("https", HOSTNAME, PORT, SERVLET, URLEncodedUtils.format(qparams, "UTF-8"), null);

		return uri;
	}

	/*
	 * This is a simpler helper function to setup the Header parameters
	 */
	private HttpPost getHeader(URI uri, long contentLength) throws UnsupportedEncodingException
	{
		HttpPost httppost = new HttpPost(uri);
		
		if( contentLength == 0 )
			;	//httppost.setHeader("Transfer-Encoding", "chunked");	//httppost.addHeader("Transfer-Encoding", "chunked");
		else
			;	//httppost.setHeader("Content-Length", Long.toString(contentLength));	//httppost.addHeader("Content-Length", Long.toString(contentLength));
		
		httppost.addHeader("Content-Type",  CODEC);
		httppost.addHeader("Content-Language", LANGUAGE);
		httppost.addHeader("Accept-Language", LANGUAGE);
		httppost.addHeader("Accept", RESULTS_FORMAT);
		httppost.addHeader("Accept-Topic", LM);

		return httppost;
	}
	
	private InputStreamEntity setAudioContent() throws NumberFormatException, Exception
	{
		File f = new File(audioFile);
		if( !f.exists() )
		{
			System.out.println("Audio file does not exist: " + audioFile);
			return null;
		}
		if( !isStreamed )
		{
			fileSize = f.length();
		}
		
		FileAudioStreamer fs = new FileAudioStreamer(audioFile, isStreamed , encodeToSpeex, Integer.parseInt(sampleRate));
		InputStreamEntity reqEntity  = new InputStreamEntity(fs.getInputStream(), -1);
		fs.start();

		reqEntity.setContentType(CODEC);

		//reqEntity.setChunked(true);

		return reqEntity;
}
	
	private void processResponse(HttpResponse response) throws IllegalStateException, IOException
	{
		HttpEntity resEntity = response.getEntity();


		System.out.println(response.getStatusLine());
		if (resEntity != null) {
			System.out.println("Response content length: " + resEntity.getContentLength());
			System.out.println("Chunked?: " + resEntity.isChunked());
			System.out.println("Nuance Session Id: " + response.getFirstHeader("x-nuance-sessionid").getValue());

			if(cookie == null){
				Header cookieHeader = response.getFirstHeader("Set-Cookie");
				cookie = cookieHeader.getValue();
				StringTokenizer st = new StringTokenizer(cookie,";");
				cookie = st.nextToken().trim();
				System.out.println("Cookie: " + cookie);
			} 
			System.out.println("------------------Result----------------------");

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(resEntity.getContent()));
			try {

				String sentence;
				int index = 1;
				while((sentence = reader.readLine()) != null){
					// do something useful with the response
					System.out.println("sentence #" + index++ + " : " + sentence);
				}
			} catch (IOException ex) {

				// In case of an IOException the connection will be released
				// back to the connection manager automatically
				throw ex;

			} catch (RuntimeException ex) {

				// In case of an unexpected exception you may want to abort
				// the HTTP request in order to shut down the underlying 
				// connection and release it back to the connection manager.
				//httppost.abort();
				throw ex;

			} finally {

				// Closing the input stream will trigger connection release
				reader.close();

			}
			resEntity.consumeContent();
		}
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception{

		DictationHTTPClient dictationHttpClient = new DictationHTTPClient();
		if( !dictationHttpClient.checkArgs(args) )
			return;
		
		try {	
			HttpClient httpclient = dictationHttpClient.getHttpClient();
			
			InputStreamEntity reqEntity = dictationHttpClient.setAudioContent();

			URI uri = dictationHttpClient.getURI();
			HttpPost httppost = dictationHttpClient.getHeader(uri, 0);	//fileSize);
			httppost.setEntity(reqEntity);			
			
			// Add a debug statement here showing all values being used for this request
			System.out.println();
			dictationHttpClient.printSettings();
			System.out.println();
			
			System.out.println("----------------- Send Audio ----------------------");
			System.out.println("executing request " + httppost.getRequestLine());
			
			HttpResponse response = httpclient.execute(httppost);
	
			System.out.println();
			System.out.println("----------------- Processing Response ----------------------");
			dictationHttpClient.processResponse(response);

		} catch(Exception e) {
			//System.out.println(e.toString());
			e.printStackTrace(System.out);
		} finally {
			// When HttpClient instance is no longer needed, 
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			if( dictationHttpClient != null && dictationHttpClient.httpclient != null )
				dictationHttpClient.httpclient.getConnectionManager().shutdown();
		}

	}

	/**
	 * Checks the command line arguments
	 * 
	 * @param args
	 * @return
	 */
	private boolean checkArgs(String[] args) {
		
		/* 
		 * -a app_id 
		 * -k app_key 
		 * -d device_id 
		 * -l lang 
		 * -f audio_format 
		 * -m language model 
		 * -o result_format 
		 * -w audio_file 
		 * -s isStreamed 
		 * -r sample_rate 
		 */
		printUsage();

		if (args.length > 20)  {
			System.err.println("Error: too many arguments.");
			printUsage();
			return false;
		}
		
		for( int i = 0; i < args.length; i=i+2)
		{
			if( args[i].equals("-a") )
				this.APP_ID = args[i+1];
			else if( args[i].equals("-k") )
				this.APP_KEY = args[i+1];
			else if( args[i].equals("-d") )
				this.DEVICE_ID = args[i+1];
			else if( args[i].equals("-l") )
				this.LANGUAGE = args[i+1];
			else if( args[i].equals("-f") )
				this.CODEC = args[i+1];
			else if( args[i].equals("-m") )
				this.LM = args[i+1];
			else if( args[i].equals("-o") )
				this.RESULTS_FORMAT = args[i+1];
			else if( args[i].equals("-w") )
				this.audioFile = args[i+1];
			else if( args[i].equals("-s") )
			{
				if( args[i+1].equals("true") )
					this.isStreamed = true;
			}
			else if( args[i].equals("-r") )
				this.sampleRate = args[i+1];
			else
			{
				System.err.println("Error: invalid flag " + args[i]);
				printUsage();
				return false;
			}
		}
		
		return true;

	}


	/**
	 * Prints the usage
	 */
	private static void printUsage(){
		/* 
		 * -a app_id 
		 * -k app_key 
		 * -d device_id 
		 * -l lang 
		 * -f audio_format 
		 * -m language model 
		 * -o result_format 
		 * -w audio_file 
		 * -s isStreamed 
		 * -r sample_rate 
		 */
		System.err.println("Usage: java -jar DictationHTTPClient.jar \n" +
				"Optional inputs:\n" +
				"\t-a app id\n" +
				"\t-k app key (128-byte string)\n" +
				"\t-d device id\n" +
				"\t-l lang (Default is en_us)\n" +
				"\t-f audio format (Default is audio/x-wav;codec=pcm;bit=16;rate=16000)\n" +
				"\t-m language model (Default is Dictation. Options are Dictation and WebSearch)\n" +
				"\t-o results format (Default is text/plain. Options are text/plain and application/xml)\n" +
				"\t-w audio file\n" +
				"\t-s isStreamed (Default is false. Options are true and false)\n" +
				"\t-r sample rate (Default is 16000. Options are 8000 and 16000\n");
	}
	private void printSettings()
	{
		System.out.println("----------------- Application Settings ----------------------");
		System.out.println("App Id: " + this.APP_ID);
		System.out.println("App Key: " + this.APP_KEY);
		System.out.println("Device Id: " + this.DEVICE_ID);
		System.out.println("Language: " + this.LANGUAGE);
		System.out.println("Language Model: " + this.LM);
		System.out.println("Audio Format: " + this.CODEC);
		System.out.println("Sample Rate: " + this.sampleRate);
		System.out.println("Audio File: " + this.audioFile);
		System.out.println("Is Streamed: " + Boolean.toString(this.isStreamed));
		System.out.println("Results Format: " + this.RESULTS_FORMAT);
		System.out.println("Host: " + DictationHTTPClient.HOSTNAME);
		System.out.println("Port: " + DictationHTTPClient.PORT);
		System.out.println("Servlet: " + DictationHTTPClient.SERVLET);
		System.out.println("-------------------------------------------------------------");
	}

}
