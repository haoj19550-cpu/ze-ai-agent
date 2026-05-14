package com.zegao.zeaiagent.demo.invok;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.Arrays;

public class HttpInvoke {

    /**
     * 使用 Hutool 工具类调用通义千问 API
     *
     * @param apiKey 阿里云 DashScope API Key
     * @return API 响应的 JSON 字符串
     */
    public static String callWithHutool(String apiKey) {
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        JSONObject requestBody = JSONUtil.createObj()
                .set("model", "qwen-max")
                .set("input", JSONUtil.createObj()
                        .set("messages", Arrays.asList(
                                JSONUtil.createObj()
                                        .set("role", "system")
                                        .set("content", "You are a helpful assistant."),
                                JSONUtil.createObj()
                                        .set("role", "user")
                                        .set("content", "你是谁？")
                        ))
                )
                .set("parameters", JSONUtil.createObj()
                        .set("result_format", "message")
                );

        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .execute();

        return response.body();
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("BAILIAN_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("错误: 请设置环境变量 DASHSCOPE_API_KEY");
            System.exit(1);
        }

        try {
            String result = callWithHutool(apiKey);
            System.out.println("调用结果:");
            System.out.println(result);
        } catch (Exception e) {
            System.err.println("调用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
