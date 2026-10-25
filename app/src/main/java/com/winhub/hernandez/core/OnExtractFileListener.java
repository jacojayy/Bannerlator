package com.winhub.hernandez.core;

import java.io.File;

public interface OnExtractFileListener {
    File onExtractFile(File destination, long size);
}
