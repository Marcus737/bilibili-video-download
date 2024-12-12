package com.wei.entity;

public class Page {
    public long cid;
    public int page;
    public String part;
    public int duration;
    public VideoInfo videoInfo;

    @Override
    public String toString() {
        return "Page{" +
                "cid=" + cid +
                ", page=" + page +
                ", part='" + part + '\'' +
                ", duration=" + duration +
                ", videoInfo=" + videoInfo +
                '}';
    }
}
