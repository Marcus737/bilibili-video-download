package com.wei.util;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.ArrayList;

public class HttpUtil {

    private static String cookie = "b_nut=1706834420; i-wanna-go-back=-1; b_ut=7; _uuid=F5C109C26-11810-D68A-2B1F-4F2E3199B810D20381infoc; enable_web_push=DISABLE; buvid4=49AAC66A-62AA-D0DC-8F79-C1F6815E033020950-024020200-ZWnx6KmmQG%2FUU6SLRIoJww%3D%3D; header_theme_version=CLOSE; LIVE_BUVID=AUTO4917068347345853; rpdid=|(ku|kmmu~)k0J'u~|YmkYYm|; buvid_fp_plain=undefined; hit-dyn-v2=1; FEED_LIVE_VERSION=V_HEADER_LIVE_NO_POP; CURRENT_BLACKGAP=0; DedeUserID=434354485; DedeUserID__ckMd5=3b42468397ac9767; fingerprint=dc904b60cb566d287fbb129c78a5109f; is-2022-channel=1; buvid_fp=cdced653cea7d208ad894af47eae747b; CURRENT_QUALITY=80; PVID=2; SESSDATA=9e2c8c34%2C1749348263%2C295d7%2Ac2CjBZRkIcA4kv9oLY5VnzbIGvd1rUhFi39XExjqfYuhnR2G8X_Wlzx_ZgKIn_pRnpJX0SVk9veE1XdUlSdTVRRC1SR3NsS1lJb21HaXhnSEh0VW5SYmpDNEhlYldZVUVQUFNsaktYdHVYdmFkb0tYd1h5SU5nZHBBSjJ3N2tZc2Y3Ym1zZXdIWGVBIIEC; bili_jct=44db566859b612d3bf44e77fbd2f9d10; home_feed_column=5; browser_resolution=1912-954; CURRENT_FNVAL=4048; sid=7gthhwml; bsource=search_bing; bili_ticket=eyJhbGciOiJIUzI1NiIsImtpZCI6InMwMyIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MzQyMzE2NjcsImlhdCI6MTczMzk3MjQwNywicGx0IjotMX0.tKBJ6_HZ9UTnsHcPsGlc_oJ7N32B2oGDmCu54KpV3NA; bili_ticket_expires=1734231607; b_lsid=57FBD2103_193B8F439E3";

    public static void setCookie(String newCookie) {
        cookie = newCookie;
    }

    public static String[] getHeaders() {
        ArrayList<String> res = new ArrayList<>();
        for (String kv : cookie.split(";")) {
            kv = kv.trim();
            String[] split = kv.split("=");
            res.add(split[0]);
            res.add(split[1]);
        }

        res.add("origin");
        res.add("https://www.bilibili.com");

        res.add("referer");
        res.add("https://www.bilibili.com");

        res.add("user-agent");
        res.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0");

        res.add("accept-language");
        res.add("zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");


        String[] ss = new String[res.size()];
        for (int i = 0; i < res.size(); i++) {
            ss[i] = res.get(i);
        }

        return ss;
    }

    public static HttpRequest getRequest(String url) {
        return  HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .headers(getHeaders())
                .build();
    }

}
