package com.pedro.encoder.ts.m3u8;

import com.pedro.encoder.ts.TsSegment;
import java.util.List;


/**
 *
 *  Media Playlist
 +++++++++++++++++++++++++++++++++++++++++++

 #EXTM3U
 #EXT-X-VERSION:3
 #EXT-X-TARGETDURATION:5
 #EXT-X-MEDIA-SEQUENCE:120

 #EXTINF:1.345,
 https://localhost/1.ts
 #EXTINF:1.311,
 https://localhost/2.ts
 #EXTINF:1.345,
 https://localhost/3.ts

 @see "https://www.jianshu.com/p/dc4e5d55758a"

 @author wangyaming
 @author xuwenfeng
 @author zhuam
 */
public class M3u8Builder {

  private int TS_DURATION = 6;

  private StringBuilder m3u8Builder = new StringBuilder(1024);

  public M3u8Builder() {}

  public M3u8Builder(int ts_duration) {
    this.TS_DURATION = ts_duration;
  }

  public M3U8 generateM3u8(long m3u8Seq, List<TsSegment> tsSegments) {

    int maxTsDuration = TS_DURATION;
    int disCountinuityEndPos = -1;

    for(int i = 0; i < tsSegments.size(); i++) {

      TsSegment ts = tsSegments.get(i);

      if(ts.isDiscontinue())
        disCountinuityEndPos = i;
      maxTsDuration = Math.max((int) Math.ceil(ts.getDuration()), maxTsDuration);
    }

    m3u8Builder.setLength(0);
    // EXTM3U must be on the first line, the identification is an Extended M3U Playlist file
    m3u8Builder.append("#EXTM3U").append("\n");													// EXTM3U 必须在第一行, 标识是一个 Extended M3U Playlist 文件
    // Indicates the compatible version of Playlist
    m3u8Builder.append("#EXT-X-VERSION:3").append("\n");
    //m3u8Builder.append("#EXT-X-PLAYLIST-TYPE:VOD").append("\n");
    // Used to specify the largest									// 表示 Playlist 兼容的版本
    m3u8Builder.append("#EXT-X-TARGETDURATION:").append(maxTsDuration).append("\n");			// 用于指定最大的 Media Segment duration
    // Used to specify the first
    m3u8Builder.append("#EXT-X-MEDIA-SEQUENCE:").append(m3u8Seq);								// 用于指定第一个 Media Segment 的 Media Sequence Number

    for(int i = 0; i < tsSegments.size(); i++) {

      TsSegment ts = tsSegments.get(i);
      m3u8Builder.append("\n");
      m3u8Builder.append("#EXTINF:").append( ts.getDuration() ).append(",").append("\n");  	//EXTINF, 用于指定 (Used to specify) Media Segment of duration
      m3u8Builder.append( ts.getName() );

      if (i == disCountinuityEndPos && disCountinuityEndPos != tsSegments.size() - 1) {
        m3u8Builder.append("\n");
        m3u8Builder.append("#EXT-X-DISCONTINUITY");											// EXT-X-DISCONTINUITY, 表示不连续 (Indicates discontinuity)
      }
    }
    byte[] buf = m3u8Builder.toString().getBytes();
    M3U8 m3u8 = new M3U8(m3u8Seq, buf, System.currentTimeMillis());
    return m3u8;
  }

}
