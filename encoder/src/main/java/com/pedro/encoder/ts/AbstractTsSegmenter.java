package com.pedro.encoder.ts;

import android.content.Context;

/**
 Show time stamp. Audio can be set to 0;
 when the video contains B frames,dts should be less than pts,
 so pts is initialized to 1 here
 Formulate a 90k Hz clock in ISO/IEC13818-1,

 if the encoding frame rate is 30,
 then the time stamp interval should be 90000/30 = 3000

 Video: pts = inc++ *(1000/fps); where inc is a static, initial value is 0,
 each time the timestamp inc is added by 1

 Audio: pts = inc++ * (frame_size * 1000 / sample_rate)
 */
public abstract class AbstractTsSegmenter {

	// Duration of TS segment, seconds
	public static final int TS_DURATION = 6;
	
	// video
	public int fps; 				//Number of frames
	
	// audio
	public float sampleRate;		//Sampling Rate
	public int sampleSizeInBits;	//Single sample size
	public int channels; 			//Number of channels
	

	// pts & dts
	protected long pts = 1L;
	protected long ptsBase = 0L;

	protected long dts = 0L;

	/*
  The number of frames contained in a single ts time period.

  Audio: Specify frameNum = ts_duration * sampleRate >> 10;

  Video: frameNum = ts_duration * fps;
  */
	protected int frameNum = 0;

	/*
  pts step size
  Audio: the number of sampling samples corresponding to
  an AAC frame/sampling frequency (unit s)

  Video: 1000/fps (unit ms)
  Conversion of pts, dts and milliseconds: According to the setting of h264,
  it is 90HZ, so the conversion formula of
  PTS/DTS to milliseconds is: ms=pts/90
  */
	protected long ptsIncPerFrame = 0;

	//The duration of the media segment
	protected float tsSegTime = 0F;
	
	public AbstractTsSegmenter() {
		
		this.sampleRate = 8000F;
		this.sampleSizeInBits = 16;
		this.channels = 1;
		
		this.fps = 10;
	}
	
	public int getFrameNum() {
		return frameNum;
	}
	
	public float getTsSegTime() {
		return tsSegTime;
	}
	
	public long getPtsIncPerFrame() {
		return ptsIncPerFrame;
	}
	
    public int calcTsNum (int length) {
        int rawFrameLen = (length >> 11) + ((length & 0x7FF) == 0 ? 0 : 1);
        return rawFrameLen / frameNum + (rawFrameLen % frameNum == 0 ? 0 : 1);
    }
    
    public float calcTsSegTime(float sampleRate) {
    	int rawFramenum = (TS_DURATION * (int)sampleRate) >> 10;
    	return 1.0F * (rawFramenum << 10) / sampleRate;
    }

	// initialization
	public void initialize(float sampleRate, int sampleSizeInBits, int channels, int fps) {
		
		this.sampleRate = sampleRate;
		this.sampleSizeInBits = sampleSizeInBits;
		this.channels = channels;
		
		this.fps = fps;
	}

	// Transcoding
	protected byte[] transcoding(byte rawDataType, byte[] rawData) {
		return rawData;
	}

	// Segmented
	protected abstract byte[] segment(byte rawDataType, byte[] rawData, byte[] reserved);
	
	
	public byte[] getTsBuf(byte rawDataType, byte[] rawData, byte[] reserved) {
		byte[] newRawData = transcoding(rawDataType, rawData );
		return segment(rawDataType, newRawData, reserved);
	}
	
	public abstract void close();
	
	public abstract void prepare4NextTs();
	
}