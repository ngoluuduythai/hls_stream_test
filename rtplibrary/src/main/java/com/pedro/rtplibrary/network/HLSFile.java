package com.pedro.rtplibrary.network;

import java.io.File;

public class HLSFile {
  private File data;
  private FileType fileType;

  public HLSFile(File data, FileType fileType){
    this.data = data;
    this.fileType = fileType;
  }

  public File getData() {
    return data;
  }

  public void setData(File data) {
    this.data = data;
  }

  public FileType getFileType() {
    return fileType;
  }

  public void setFileType(FileType fileType) {
    this.fileType = fileType;
  }
}

