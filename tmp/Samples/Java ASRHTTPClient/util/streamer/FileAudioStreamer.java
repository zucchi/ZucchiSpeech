package util.streamer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import util.audio.JSpeexEnc;

/*
 * Nuance Communications Inc.
 * 
 */
public class FileAudioStreamer extends Thread {

	private PipedInputStream pipedIn;
	private PipedOutputStream pipedOut;
	private final byte[] audio;
	private int curPos = 0;	
	private JSpeexEnc encoder;
	private boolean isStreamed;
	private boolean encodeToSpeex;
	private int sampleRate;

	public FileAudioStreamer(String path, boolean isStreamed, boolean encodeToSpeex, int sampleRate) throws Exception {
		this.audio = loadBytesFromFile(new File(path));
		this.isStreamed = isStreamed;
		this.encodeToSpeex = encodeToSpeex;
		this.sampleRate = sampleRate;
		
		if(encodeToSpeex)
			this.encoder = new JSpeexEnc(sampleRate); //PCM 8K only for now
	}

	public byte[] getAudio(){		
		return audio;
	}

	public void run(){
		try {

			if(isStreamed){
				System.out.println("Streaming started ...");
				int frameSize = sampleRate * 2 / 50;
				System.out.println("frame size:" + frameSize);
				while(hasMoreFrame()){
					pipedOut.write(encodeToSpeex ? encoder.encode(getNextChunk(frameSize)) : getNextChunk(frameSize));
					Thread.sleep(20);
				}
				System.out.println("Streaming completed ...");
			} else {
				pipedOut.write(encodeToSpeex ? encoder.encode(getAudio()): getAudio());
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				pipedOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	/**
	 * @param maxSize
	 * @return
	 */
	public byte[] getNextChunk(int maxSize) {
		int len = audio.length - curPos <= maxSize ? audio.length - curPos: maxSize;
		byte[] chunk = new byte[len];
		System.arraycopy(audio, curPos, chunk, 0, len);
		curPos += len;
		return chunk;
	}

	/**
	 * @return
	 */
	public boolean hasMoreFrame() {
		return curPos != audio.length;
	}

	/**
	 * Loads the file into memory
	 * 
	 * Note:If Java runs out of memory increase the heap size.
	 * java -Xms<initial heap size> -Xmx<maximum heap size>
	 * 
	 * @param file
	 * @return
	 */
	public byte[] loadBytesFromFile(File file) {
		WritableByteChannel outputChannel = null;
		FileChannel in = null;
		try {
			FileInputStream input = new FileInputStream(file);
			in = input.getChannel();
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			outputChannel = Channels.newChannel(out);
			in.transferTo(0, in.size(), outputChannel);

			return out.toByteArray();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				if (outputChannel != null)
					outputChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new byte[0];
	}

	public InputStream getInputStream() throws IOException {
		pipedOut = new PipedOutputStream();
		pipedIn = new PipedInputStream(pipedOut); 
		return pipedIn;


	}


}
