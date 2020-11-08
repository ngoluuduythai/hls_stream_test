package com.pedro.encoder.ts;

import android.content.Context;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author xuwenfeng@variflight.com
 */
public class H264TsSegmenter extends AbstractTsSegmenter {

  //NAL(Network Abstraction Layer) type
  private static final int H264NT_SLICE = 1;
  private static final int H264NT_SLICE_IDR = 5; //IDR (Instantaneous Decoding Refresh) frame
  //SPS (Sequence Parameter Set) sequence parameter set
  private static final int H264NT_SPS = 7;  //SPS type value
  //PPS (Picture Parameter Set) image parameter set
  private static final int H264NT_PPS = 8;  //PPS type valueI
  private static final int H264NT_UNUSED_TYPE = 0; //Unused type

  //Frame type
  private static final int FRAME_I = 15;
  private static final int FRAME_P = 16;
  private static final int FRAME_B = 17;
  private static final int UNSUPPORT = -1;

  /***
   * Since the video data may be greater than 65535,
   *  set pes_packet_length = 0x0000 (video only) when TsEncode
   */
  private static final int TWO_FRAME_MAX_SIZE = 65535 * 2;

  /***
   * If the slice corresponding to NALU is the beginning of a frame,
   *  it is represented by 4 bytes, ie 0x00000001;
   *  otherwise, it is represented by 3 bytes, 0x000001
   *
   *  start code to split, start code as a delimiter
   */
  private static final byte[] NAL_DELIMITER = { 0x00, 0x00, 0x01 };

  private boolean isFirstPes = true;
  private boolean waitingIDRFrame = true;        //Wait for key frame
  private int currentNalType = 0;
      //The currently encoded frame type (only I/B/P frames are recorded)
  private int numInGop = 0;              //NumInGop frame in the current frame group

  private byte[][] tsSecs;
      // A tsSegment contains several tsSecs: {tsSegment} = {[tsSec] [tsSec] ... [tsSec]}
  private int tsSecsPtr = 0;              // tsSecs 指针
  private int tsSegmentLen = 0;            // The number of bytes of a tsSegment

  private RingBuffer framesBuf;
      //Used to buffer the original data from the video stream, there may be multiple frames

  private ArrayDeque<AvcFrame> avcFrameCache = new ArrayDeque<AvcFrame>();
  private List<AvcFrame> cacheAvcFrames = new ArrayList<AvcFrame>();

  private TsWriter tsWriter;
  private TsEncoder tsEncoder;

  public H264TsSegmenter() {

    super();

    frameNum = (int) (TS_DURATION * this.fps - 1);
    ptsIncPerFrame = (long) (1000 / this.fps) * 90;
    pts += ptsIncPerFrame;
    dts = pts - 200;
    tsSegTime = frameNum * ptsIncPerFrame / 1000F;  //默认值

    tsWriter = new TsWriter();
    tsEncoder = new TsEncoder();

    framesBuf = new RingBuffer(TWO_FRAME_MAX_SIZE);
    tsSecs = new byte[3000][];

    prepare4NextTs();
  }

  @Override
  public void initialize(float sampleRate, int sampleSizeInBits, int channels, int fps) {
    this.fps = fps;
    frameNum = (int) (TS_DURATION * this.fps - 1);
    ptsIncPerFrame = (long) (1000 / this.fps) * 90;
    pts += ptsIncPerFrame;
    dts = pts - 200;
    tsSegTime = frameNum * ptsIncPerFrame / 1000F;  //Defaults
  }

  public void resetPts(long pts) {
    this.pts = pts;
    this.ptsBase = 0L;
  }

  public void prepare4NextTs() {

    numInGop = 0;
    tsSecsPtr = 0;
    tsSegmentLen = 0;

    tsWriter.reset();
    tsEncoder.close();

    avcFrameCache.clear();

    for (int i = 0; i < tsSecs.length; i++) {
      tsSecs[i] = null;
    }
  }

  private void writeTsFile(
      String pathname,
      byte[] buf, Context context
  ) {
    FileOutputStream fos = null;
    try {
      fos = context.openFileOutput(pathname, Context.MODE_APPEND);
      fos.write(buf);
      fos.flush();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (fos != null) {
          fos.close();
        }
      } catch (Exception ex) {
      }
    }
  }


  public void haveIDRFrame(byte[] rawData){
    byte[] tsSegment = null;
    boolean isNalDelimiter4 = false;
    byte nextNalType = H264NT_UNUSED_TYPE;
    framesBuf = new RingBuffer(TWO_FRAME_MAX_SIZE);

    System.out.println("xxx rawData: " + rawData.length);


    int seekPos =
        framesBuf.size() >= NAL_DELIMITER.length ? framesBuf.size() - NAL_DELIMITER.length : 0;
    System.out.println("xxx nextNalType: " + seekPos);
    System.out.println("xxx framesBuf: " + framesBuf.size());


    framesBuf.add(rawData);
    byte[] src = framesBuf.elements(seekPos, framesBuf.size() - seekPos); //may be remain
    System.out.println("xxx src: " + src.length);

    //NAL separator position
    List<Integer> delimiters = ByteUtil.kmp(src, NAL_DELIMITER);

    for (int i = 0; i < delimiters.size(); i++) {

      //Frame type of the next frame
      if (delimiters.get(i) + NAL_DELIMITER.length < src.length) {
        nextNalType = src[delimiters.get(i) + NAL_DELIMITER.length];
        System.out.println("xxx nextNalType index "+ i +" : " + nextNalType);
      } else {
        break;
      }

      System.out.println("xxx isIDRFrame: " + isIDRFrame(nextNalType));
    }
  }

  @Override
  protected byte[] segment(byte rawDataType, byte[] rawData, byte[] reserved) {

    byte[] tsSegment = null;
    boolean isNalDelimiter4 = false;
    byte nextNalType = H264NT_UNUSED_TYPE;
    System.out.println("xxx rawData: " + rawData.length); //1220697


    int seekPos =
        framesBuf.size() >= NAL_DELIMITER.length ? framesBuf.size() - NAL_DELIMITER.length : 0;
    System.out.println("xxx seekPos: " + seekPos); //start point 0
    System.out.println("xxx framesBuf: " + framesBuf.size()); // 0
    System.out.println("xxx NAL_DELIMITER.length: " + NAL_DELIMITER.length);

    framesBuf.add(rawData);

    byte[] src = framesBuf.elements(seekPos, framesBuf.size() - seekPos); //may be remain
    System.out.println("xxx src: " + src.length); // [seekPos][framesBuf.size() - seekPos ]

    //NAL separator position
    List<Integer> delimiters = ByteUtil.kmp(src, NAL_DELIMITER);
    System.out.println("xxx delimiters size: " + delimiters.size()); // find NAL NAL NAL NAL separator position
    //306
    //NAL start code

    for (int i = 0; i < delimiters.size(); i++) {

      System.out.println("xxx NAL separator position: " + i);
      System.out.println("xxx Frame type of the next frame");

      //Frame type of the next frame
      // NAL + NAL_DELIMITER < src
      if (delimiters.get(i) + NAL_DELIMITER.length < src.length) {
        nextNalType = src[delimiters.get(i) + NAL_DELIMITER.length];
        System.out.println("xxx nextNalType index "+ i +" : " + nextNalType);
        System.out.println("xxx nextNalType index & & 0x1F: " + (nextNalType & 0x1F));

      } else {
        break;
      }

      //Take the end position of the current complete frame
      int endPos = i == 0 ? seekPos + delimiters.get(0)
          : delimiters.get(i) - delimiters.get(i - 1) + (isNalDelimiter4 ? 1 : 0);
      // i == 0 thi la seekPos + delimiters.get(0)
      // i khac 0 NAL(i) - NAL(i-1) + (isNalDelimiter4 ? 1 : 0)
      // endPos end cua mot NAL


      //Determine whether the separator is 0x00000001
      //NAL start code
      System.out.println("xxx Determine whether the separator is 0x00000001");

      isNalDelimiter4 =
          (endPos != 0 && i == 0 && delimiters.get(i) != 0 && src[delimiters.get(i) - 1] == 0x00);
      // id endPOS khac 0  va i = 0 va NAL tai i khac 0 va src[Nal(i) - 1) = 0x00

      System.out.println("xxx isNalDelimiter4: "+ isNalDelimiter4);
      endPos = isNalDelimiter4 ? endPos - 1 : endPos;


      if (waitingIDRFrame) {
        //Determine whether the next time is an IDR frame
        System.out.println("xxx isIDRFrame: " + isIDRFrame(nextNalType));
        if (isIDRFrame(nextNalType)) {
          waitingIDRFrame = false;
        } else {
          //Remove incomplete frame data before IDR frame
          framesBuf.remove(0, endPos);
          continue;
        }
      }


      switch (currentNalType){
        case 1: {
          System.out.println("xxx currentNalType: H264NT_SLICE");
          break;
        }
        case 5: {System.out.println("xxx currentNalType: H264NT_SLICE_IDR");
          break;
        }
        default: {System.out.println("xxx currentNalType if " + currentNalType);
          break;
        }
      }


      if (currentNalType == H264NT_SLICE || currentNalType == H264NT_SLICE_IDR) {

        //Get the complete frame data (the IDR frame and the SPS+PPS before the IDR frame are a whole)
        byte[] avcBuf = framesBuf.remove(0, endPos);


        if (avcBuf != null && avcBuf.length > NAL_DELIMITER.length) {

          System.out.println("xxx avcBuf: " + avcBuf.length);

          boolean isLastFrame = (nextNalType & 0x1F) == H264NT_SPS;
          System.out.println("xxx isLastFrame: " + isLastFrame);
          System.out.println("xxx isLastFrame: " + (nextNalType & 0x1F));


          int frameType = H264NalUtil.getPesFrameType(avcBuf);
          System.out.println("xxx get PES FrameType: " + frameType);

          List<AvcFrame> encodeAvcFrames = getEncodeAvcFrames(
              new AvcFrame(avcBuf, frameType, -1, getDts()), isLastFrame);
          numInGop++;

          System.out.println("xxx encodeAvcFrames size: " + encodeAvcFrames.size());
          for (AvcFrame avcFrame : encodeAvcFrames) {

            System.out.println("xxx isFirstPes: " + isFirstPes);

            byte[] tsBuf = tsWriter.writeH264(isFirstPes, avcFrame.payload, avcFrame.payload.length,
                avcFrame.pts, avcFrame.dts);

            //writeTsFile(System.nanoTime()/1000+"h264.ts", tsBuf, context);

            //byte[] tsBuf = tsEncoder.encode(false, avcFrame.payload, avcFrame.payload.length, avcFrame.pts, avcFrame.dts, true, isFirstPes);
            isFirstPes = false;

            if (tsBuf != null) {
              tsSegmentLen += tsBuf.length;
              tsSecs[tsSecsPtr++] = tsBuf;
              //tsWriter.reset();
              tsEncoder.close();
            }
          }

          tsSegTime = numInGop / this.fps; // so frame trong frameGroup / frames/s = 10s

          System.out.println("xxx tsSegTime: " + tsSegTime);
          System.out.println("xxx isLastFrame: " + isLastFrame);
          System.out.println("xxx if: " + (isLastFrame && tsSegTime >= 3F));


          if (isLastFrame && tsSegTime >= 6F) {
            waitingIDRFrame = true;
            isFirstPes = true;
            tsSegTime = numInGop / this.fps;
            tsSegment = new byte[tsSegmentLen];
            int tsSegmentPtr = 0;

            for (int j = 0; j < tsSecs.length; j++) {
              if (tsSecs[j] != null) {
                System.arraycopy(tsSecs[j], 0, tsSegment, tsSegmentPtr, tsSecs[j].length);
                tsSegmentPtr += tsSecs[j].length;
              }
            }


            System.out.println("xxx tsSegment: " + (tsSegment.length));


            //framesBuf = new RingBuffer(TWO_FRAME_MAX_SIZE);
            prepare4NextTs();
          }
        }
      }


      switch ((nextNalType & 0x1F)){
        case 1: {System.out.println("xxx nextNalType: H264NT_SLICE");
          break;
        }
        case 5: {System.out.println("xxx nextNalType: H264NT_SLICE_IDR");
          break;
        }
        default: {System.out.println("xxx nextNalType if " + (nextNalType & 0x1F));
          break;
        }
      }

      //Update currentNalType
      if (((nextNalType & 0x1F) == H264NT_SLICE_IDR) || ((nextNalType & 0x1F) == H264NT_SLICE)) {
        System.out.println("xxx Update currentNalType: " + (nextNalType & 0x1F));
        currentNalType = nextNalType & 0x1F;
      }

      System.out.println("xxx currentNalType 2: " + currentNalType);

      if (waitingIDRFrame) {

        System.out.println("xxx set currentNalType == 0: ");

        currentNalType = 0;
      }

      System.out.println("xxx currentNalType 3: " + currentNalType);
    }

    return tsSegment;
  }

  //mixed itf
  public AvcResult process(byte[] rawData) {

    boolean isNalDelimiter4 = false;
    byte nextNalType = H264NT_UNUSED_TYPE;

    int seekPos =
        framesBuf.size() >= NAL_DELIMITER.length ? framesBuf.size() - NAL_DELIMITER.length : 0;

    framesBuf.add(rawData);
    byte[] src = framesBuf.elements(seekPos, framesBuf.size() - seekPos);

    List<Integer> delimiters = ByteUtil.kmp(src, NAL_DELIMITER);
    List<AvcFrame> encodeAvcFrames = new ArrayList<AvcFrame>();
    List<AvcFrame> endAvcFrames = new ArrayList<AvcFrame>();
    encodeAvcFrames.addAll(cacheAvcFrames);
    cacheAvcFrames.clear();
    boolean isTailAvc = false;

    for (int i = 0; i < delimiters.size(); i++) {
			if (delimiters.get(i) + NAL_DELIMITER.length < src.length) {
				nextNalType = src[delimiters.get(i) + NAL_DELIMITER.length];
			} else {
				break;
			}

      int endPos = i == 0 ? seekPos + delimiters.get(0)
          : delimiters.get(i) - delimiters.get(i - 1) + (isNalDelimiter4 ? 1 : 0);

      isNalDelimiter4 =
          (endPos != 0 && i == 0 && delimiters.get(i) != 0 && src[delimiters.get(i) - 1] == 0x00);
      endPos = isNalDelimiter4 ? endPos - 1 : endPos;

      //if (waitingIDRFrame) {
			//	if (isIDRFrame(nextNalType)) {
			//		waitingIDRFrame = false;
			//	} else {
			//		framesBuf.remove(0, endPos);
			//		continue;
			//	}
      //}

      if (currentNalType == H264NT_SLICE || currentNalType == H264NT_SLICE_IDR) {

        byte[] avcBuf = framesBuf.remove(0, endPos);

        if (avcBuf != null && avcBuf.length > NAL_DELIMITER.length) {

          boolean isLastFrame = (nextNalType & 0x1F) == H264NT_SPS;
          int frameType = H264NalUtil.getPesFrameType(avcBuf);

          encodeAvcFrames.addAll(
              getEncodeAvcFrames(new AvcFrame(avcBuf, frameType, -1, getDts()), isLastFrame));


          if (!isTailAvc && isLastFrame) {
            endAvcFrames.addAll(encodeAvcFrames);
            encodeAvcFrames.clear();
            isTailAvc = true;
            waitingIDRFrame = true;
          }
        }
      }
      if (((nextNalType & 0x1F) == H264NT_SLICE_IDR) || ((nextNalType & 0x1F) == H264NT_SLICE)) {
        currentNalType = nextNalType & 0x1F;
      }

			if (waitingIDRFrame) {
				currentNalType = 0;
			}
    }
    return endAvcFrames.isEmpty() && encodeAvcFrames.isEmpty() ? null
        : new AvcResult(isTailAvc ? endAvcFrames : encodeAvcFrames, isTailAvc);
  }

  @Override
  public void close() {

    if (tsSecs != null) {
      tsSecs = null;
    }
  }

  private List<AvcFrame> getEncodeAvcFrames(AvcFrame avcFrame, boolean isLastFrame) {
    List<AvcFrame> avcFrames = new ArrayList<AvcFrame>();

    switch (avcFrame.frameType) {
      case FRAME_I:
      case FRAME_P:
      case UNSUPPORT:
        if (!avcFrameCache.isEmpty()) {
          AvcFrame avcFrame2 = avcFrameCache.pop();
          avcFrame2.pts = getPts();
          avcFrames.add(avcFrame2);
					while (!avcFrameCache.isEmpty()) {
						avcFrames.add(avcFrameCache.pop());
					}
        }
        break;

      case FRAME_B:
        avcFrame.pts = getPts();
        break;
    }

    avcFrameCache.offer(avcFrame);

    if (isLastFrame) {

      AvcFrame avcFrame2 = avcFrameCache.pop();
      avcFrame2.pts = getPts();
      avcFrames.add(avcFrame2);
			while (!avcFrameCache.isEmpty()) {
				avcFrames.add(avcFrameCache.pop());
			}
    }

    return avcFrames;
  }

  private long getPts() {
    return pts += ptsIncPerFrame;
  }

  private long getDts() {
    return dts += ptsIncPerFrame;
  }

  private boolean isIDRFrame(byte nalType) {
    return (nalType & 0x1F) == H264NT_SPS
        || (nalType & 0x1F) == H264NT_PPS
        || (nalType & 0x1F) == H264NT_SLICE_IDR;
  }

  public static class AvcResult {

    public List<TsWriter.FrameData> avcFrames = new ArrayList<TsWriter.FrameData>();
    public boolean isTailAvc;

    public AvcResult(List<AvcFrame> avcFrames, boolean isTailAvc) {
      for (AvcFrame frame : avcFrames) {
        TsWriter.FrameData frameData = new TsWriter.FrameData();
        frameData.buf = frame.payload;
        frameData.pts = frame.pts;
        frameData.dts = frame.dts;
        frameData.isAudio = false;
        this.avcFrames.add(frameData);
      }
      this.isTailAvc = isTailAvc;
    }
  }

  public static class AvcFrame {

    public byte[] payload;
    public int frameType;
    public long pts = -1;
    public long dts = -1;

    public AvcFrame(byte[] payload, int frameType, long pts, long dts) {
      this.payload = payload;
      this.frameType = frameType;
      this.pts = pts;
      this.dts = dts;
    }
  }

  static class RingBuffer {
    private static final int capaStepLen = 5000; //Expansion step
    private int capacity = 0;
    private int headPtr = 0;
    private int tailPtr = 0;

    private byte[] elementData;

    public RingBuffer(int capacity) {
      this.capacity = capacity;
      elementData = new byte[capacity];
      for (int i = 0; i < capacity; i++) {
        elementData[i] = (byte) 0xFF;
      }
    }

    public int size() {
      return (tailPtr - headPtr + capacity) % capacity;
    }

    public boolean isEmpty() {
      return size() == 0;
    }

    public boolean isFull() {
      return (tailPtr + 1) % capacity == headPtr;
    }

    private void add(byte element) {
			if (isFull()) {
				expandCapacity();
			}

      elementData[tailPtr] = element;
      tailPtr = (tailPtr + 1) % capacity;
    }

    private byte remove() {
			if (isEmpty()) {
				throw new NoSuchElementException("The buffer is already empty");
			}
      byte element = elementData[headPtr];
      headPtr = (headPtr + 1) % capacity;
      return element;
    }

    public void add(byte[] elements) {
      for (byte element : elements) {
        add(element);
      }
    }

    public byte[] remove(int len) {
			if (len > size()) {
				return null;
			}

      byte[] elements = new byte[len];
      for (int i = 0; i < len; i++) {
        byte element = remove();
        elements[i] = element;
      }
      return elements;
    }

    //[0-from)Discard the data, then remove the len bit data and return, from counts from 0
    public byte[] remove(int from, int len) {
			if (from > size()) {
				return null;
			}

      headPtr = (headPtr + from) % capacity;
      return remove(len);
    }

    public byte[] elements() {
      byte[] elements = new byte[size()];
      for (int i = 0; i < size(); i++) {
        elements[i] = elementData[(headPtr + i) % capacity];
      }
      return elements;
    }

    //from is calculated from 0
    public byte[] elements(int from, int len) {
			if (from + len > size()) {
				return null;
			}
      byte[] elements = new byte[len];
      for (int i = 0; i < len; i++) {
        elements[i] = elementData[(headPtr + from + i) % capacity];
      }
      return elements;
    }

    //pos is calculated from 0
    public byte get(int pos) {
      return elementData[(headPtr + pos) % capacity];
    }

    private void expandCapacity() {
      byte[] copy = Arrays.copyOf(elementData, capacity + capaStepLen);
      if (tailPtr < headPtr) {
        System.arraycopy(elementData, headPtr, copy, headPtr + capaStepLen, capacity - headPtr);
        headPtr += capaStepLen;
      }
      capacity += capaStepLen;
      elementData = copy;
    }
  }

  public static void main(String[] args) {
    byte[] a = { 0x00, 0x00, 0x01 };
    byte[] b = {
        0x00, 0x00, 0x01, 0x41, 0x16, (byte) 0x90, (byte) 0xFE, (byte) 0xB1,
        0x5F, 0x5F, 0x00, 0x00, 0x00, 0x01, 0x41, (byte) 0xE0,
        0x5F, 0x5F, 0x00, 0x00, 0x00, 0x01, 0x41, (byte) 0xE0,
        (byte) 0xD1, 0x0B, (byte) 0xFF, (byte) 0x99, (byte) 0xC5,
        0x5F, 0x5F, 0x00, 0x00, 0x00, 0x01, 0x41, (byte) 0xE0
    };
    List<Integer> res = ByteUtil.kmp(b, a);
    System.out.println("kmp: " + res);
    for (int i = 0; i < res.size(); i++) {
      for (int j = 0; j < a.length; j++)
        System.out.print(b[res.get(i) + j]);
      System.out.println();
    }

    System.out.println();
    RingBuffer ringBuf = new RingBuffer(100);
    ringBuf.add(a);
    ringBuf.add(b);

    System.out.println("base data");

    for (byte element : a) {
      System.out.print(element + " ");
    }
    for (byte element : b) {
      System.out.print(element + " ");
    }

    System.out.println();
    System.out.println("elements()");
    byte[] datas = ringBuf.elements();
    for (byte element : datas) {
      System.out.print(element + " ");
    }

    System.out.println();
    System.out.println("elements(2, 5)");
    datas = ringBuf.elements(2, 5);
    for (byte element : datas) {
      System.out.print(element + " ");
    }

    System.out.println();
    System.out.println("get(2)");
    System.out.println(ringBuf.get(2));

    System.out.println("remove(3)");
    datas = ringBuf.remove(3);
    for (byte data : datas)
      System.out.print(data);

    System.out.println();
    System.out.println("remove(1, 5)");
    datas = ringBuf.remove(1, 5);
    for (byte data : datas)
      System.out.print(data + " ");
    System.out.println();
  }
}
