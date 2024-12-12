package com.wei.entity;

import java.util.List;

public class VideoInfo{
    public List<String> videoQualityText;
    public List<Integer> videoQualityCode;


    @Override
    public String toString() {
        return "VideoInfo{" +
                "videoQualityText=" + videoQualityText +
                ", videoQualityCode=" + videoQualityCode +
                '}';
    }
}
