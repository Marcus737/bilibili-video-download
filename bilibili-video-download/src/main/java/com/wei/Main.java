package com.wei;

import com.wei.entity.Page;
import com.wei.entity.VideoInfo;
import com.wei.util.AVBVConverter;
import com.wei.util.BiliTicket;
import com.wei.util.HttpUtil;
import com.wei.util.WbiEnc;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class Main {


    public static HttpClient client = HttpClient.newBuilder().executor(Executors.newFixedThreadPool(16)).build();
    public static ConcurrentHashMap<File, Long> map = new ConcurrentHashMap<>();
    public static Scanner sc = new Scanner(System.in);

    /**
     * 获取bvid下的视频信息
     *
     * @param bvid bvid
     * @return page列表
     * @throws Exception 异常
     */
    public static List<Page> getVideoPageList(String bvid) throws Exception {
        //不要重复join一个线程，第二次join会立即返回

        //获取基本的Page信息
        String bvidEnc = URLEncoder.encode(bvid, StandardCharsets.UTF_8);
        String url = String.format("https://api.bilibili.com/x/player/pagelist?bvid=%s", bvidEnc);
        HttpRequest request = HttpUtil.getRequest(url);
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        String json = new String(response.body(), StandardCharsets.UTF_8);


        ArrayList<Page> pageArrayList = getPages(json);

        //使用CountDownLatch进行同步
        CountDownLatch countDownLatch = new CountDownLatch(pageArrayList.size());
        //完善Page信息
        for (Page page : pageArrayList) {
            //获取清晰度
            CompletableFuture<HttpResponse<byte[]>> future = getPlayUrlMP4(bvid, page.cid, 116, 1, 0, 0);

            future.thenApply(HttpResponse::body)
                    .thenAccept(bytes -> {
                        String resp = new String(bytes, StandardCharsets.UTF_8);
//                        System.out.println(resp);
                        JSONObject jo = new JSONObject(resp);
                        JSONObject data = jo.getJSONObject("data");
                        JSONArray acceptDescription = data.getJSONArray("accept_description");
                        JSONArray acceptQuality = data.getJSONArray("accept_quality");
//                        System.out.println(acceptQuality);
//                        System.out.println(acceptDescription);

                        VideoInfo videoInfo = new VideoInfo();

                        List<String> adList = new ArrayList<>(acceptDescription.length());
                        for (Object o : acceptDescription) {
                            adList.add((String) o);
                        }
//                        System.out.println(adList);

                        List<Integer> aqList = new ArrayList<>(acceptQuality.length());
                        for (Object o : acceptQuality) {
                            aqList.add((Integer) o);
                        }
//                        System.out.println(aqList);

                        videoInfo.videoQualityText = adList;
                        videoInfo.videoQualityCode = aqList;
                        page.videoInfo = videoInfo;

                        countDownLatch.countDown();
                    });
        }
        countDownLatch.await();
        return pageArrayList;
    }

    private static ArrayList<Page> getPages(String json) {
        JSONObject jo = new JSONObject(json);

        JSONArray data = jo.getJSONArray("data");

        ArrayList<Page> pages = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject pageJo = data.getJSONObject(i);
            Page page = new Page();
            page.page = pageJo.getInt("page");
            page.cid = pageJo.getLong("cid");
            page.duration = pageJo.getInt("duration");
            page.part = pageJo.getString("part");
            pages.add(page);
        }
        return pages;
    }


    public static CompletableFuture<HttpResponse<byte[]>> getPlayUrlMP4(String bvid, long cid, int qn, int fnval, int fnver, int fourk) throws Exception {
        String[] imgKeyAndSubKey = BiliTicket.getImgKeyAndSubKey();
        String imgKey = imgKeyAndSubKey[0];
        String subKey = imgKeyAndSubKey[1];
        String mixinKey = WbiEnc.getMixinKey(imgKey, subKey);

        // 用TreeMap自动排序
        TreeMap<String, Object> map = new TreeMap<>();
        map.put("bvid", bvid);
        map.put("cid", cid);
        map.put("qn", qn);
        map.put("fnval", fnval);
        map.put("fnver", fnver);
        map.put("fourk", fourk);
        map.put("wts", System.currentTimeMillis() / 1000);
        String param = map.entrySet().stream()
                .map(it -> String.format("%s=%s", it.getKey(), WbiEnc.encodeURIComponent(it.getValue())))
                .collect(Collectors.joining("&"));
        String s = param + mixinKey;

        String wbiSign = WbiEnc.md5(s);

        String finalParam = param + "&w_rid=" + wbiSign;


        String url = String.format("https://api.bilibili.com/x/player/wbi/playurl?%s", finalParam);


        HttpRequest request = HttpUtil.getRequest(url);

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());

    }

    public static void download(String bvid, long cid, int qn, String filename) throws Exception {

        CompletableFuture<HttpResponse<byte[]>> future = getPlayUrlMP4(bvid, cid, qn, 1, 0, 0);
        future.thenApply(HttpResponse::body)
                .thenAccept(bytes -> {
                    String mp4Info = new String(bytes, StandardCharsets.UTF_8);
//                    System.out.println(mp4Info);
                    JSONObject mp4InfoJo = new JSONObject(mp4Info);
                    JSONObject data = mp4InfoJo.getJSONObject("data");
                    JSONArray durlList = data.getJSONArray("durl");
                    JSONObject dualJo = durlList.getJSONObject(0);
                    String mp4Filename = String.format("music/%s.mp4", filename);
//
//                    System.out.println(mp4Filename);

                    File file = new File(mp4Filename);

                    String url = dualJo.getString("url");
                    long videoSize = dualJo.getLong("size");
                    map.put(file, videoSize);

                    System.out.printf("正在下载：%s 总大小：%.2f mib\n", filename, (double)videoSize /1024/1024);
//                    System.out.println(url);
                    HttpRequest request = HttpUtil.getRequest(url);
                    client.sendAsync(request, HttpResponse.BodyHandlers.ofFile(new File(mp4Filename).toPath()))
                            .thenApply(HttpResponse::body);

                });
    }


    public static void search(String bvid) throws Exception {
        System.out.println("信息正在加载\n");
        //获取该bv下所有的视频
        List<Page> videoPageList = getVideoPageList(bvid);
        boolean batchDownload = false;
        if (videoPageList.size() > 1) {
            System.out.println("存在多个视频是否批量下载？ yes or no");
            batchDownload = sc.nextLine().trim().equals("yes");

        }

        if (batchDownload) {
            System.out.println("选择最高清晰度，输入编号");
            List<String> text = videoPageList.get(0).videoInfo.videoQualityText;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < text.size(); i++) {
                sb.append("清晰度：").append(text.get(i)).append("\t").append("编号：").append(i).append("\n");
            }
            System.out.println(sb);
            String id = sc.nextLine().trim();
            int idInt;
            try {
                idInt = Integer.parseInt(id);
            }catch (NumberFormatException e) {
                System.out.println("请输入数字");
                return;
            }
            if (idInt < 0 || idInt > videoPageList.size()) {
                System.out.println("请输入合理的范围");
                return;
            }
            for (Page page : videoPageList) {
                download(bvid, page.cid, page.videoInfo.videoQualityCode.get(idInt), page.part);
            }
        }else {
            for (Page page : videoPageList) {
                if (page.videoInfo == null) {
                    System.out.println("获取视频清晰度失败");
                    continue;
                }
                System.out.printf("\n视频标题：%s\n视频长度（秒）：%s\n清晰度：%s\n", page.part, page.duration, page.videoInfo.videoQualityText);
                System.out.println("选择要下载的清晰度，输入清晰度的编号，输入s跳过本视频，输入e结束下载");
                StringBuilder sb = new StringBuilder();
                List<String> text = page.videoInfo.videoQualityText;
                for (int i = 0; i < text.size(); i++) {
                    sb.append("清晰度：").append(text.get(i)).append("\t").append("编号：").append(i).append("\n");
                }
                System.out.println(sb);
                String id = sc.nextLine().trim();
                if (id.equals("s")) {
                    continue;
                } else if (id.equals("e")) {
                    break;
                }
                while (true) {
                    if (id.equals("s")) {
                        break;
                    }
                    int idInt;
                    try {
                        idInt = Integer.parseInt(id);
                    } catch (NumberFormatException e) {
                        System.out.println("\n请输入数字，输入s跳过本视频");
                        id = sc.nextLine().trim();
                        continue;
                    }
                    if (idInt < 0 || idInt > videoPageList.size()) {
                        System.out.println("\n输入编号范围错误，请重新输入，输入s跳过本视频");
                        id = sc.nextLine().trim();
                    } else {
                        download(bvid, page.cid, page.videoInfo.videoQualityCode.get(idInt), page.part);
                        break;
                    }

                }
            }
        }

    }

    public static void main(String[] args) throws Exception {

        File music = new File("music");
        if (!music.exists()) {
            music.mkdir();
        }

        while (true) {
            System.out.println("\n输入url或bvid或avid：获取视频\n输入ls：获取下载列表\n输入cookie：更新cookie\n输入exit：退出\n");
            String line = sc.nextLine().trim();
            if (line.equals("exit")) {
                break;
            }else if (line.equals("cookie")) {
                System.out.println("请复制浏览器的cookie粘贴到此处");
                String cok = sc.nextLine().trim();
                HttpUtil.setCookie(cok);
                System.out.println("已更新cookie");
            }
            else if (line.startsWith("http")) {
                String bvid;
                int av = line.indexOf("av");
                if (av != -1) {
                    StringBuilder avBuilder = new StringBuilder();
                    for (int i = av + 2; i < line.length(); i++) {
                        if (Character.isDigit(line.charAt(i))) {
                            avBuilder.append(line.charAt(i));
                        } else {
                            break;
                        }
                    }
                    System.out.println("找到av号：" + avBuilder);
                    bvid = AVBVConverter.av2bv(Long.parseLong(avBuilder.toString()));
                    search(bvid);
                } else {
                    int bv = line.indexOf("BV");
                    if (bv == -1) {
                        System.out.println("未找到av号或bv号");
                    }else {
                        StringBuilder bvBuilder = new StringBuilder();
                        for (int i = bv; i < line.length(); i++) {
                            if (Character.isDigit(line.charAt(i)) || Character.isLetter(line.charAt(i))) {
                                bvBuilder.append(line.charAt(i));
                            } else {
                                break;
                            }
                        }
                        System.out.println("找到bv号：" + bvBuilder);
                        search(bvBuilder.toString());
                    }
                }
            }else if (line.startsWith("av")) {
                long aid;
                try {
                    aid = Long.parseLong(line.substring(3));
                } catch (NumberFormatException e) {
                    System.out.println("输入avid有误");
                    continue;
                }
                String bv = AVBVConverter.av2bv(aid);
                search(bv);
            } else if (line.startsWith("BV")) {
                search(line);
            } else if (line.equals("ls")) {
                map.forEach((k,v) -> {
                    try {
                        System.out.printf("%s\t下载进度:%d\n", k.getName(), (int)((double)Files.size(k.toPath()) / v * 100));
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
//                        throw new RuntimeException(e);
                    }
                });
            } else {
                System.out.println("未知指令");
            }
        }
        ExecutorService executorService = (ExecutorService) client.executor().get();
        executorService.shutdown();
        sc.close();
    }

}
