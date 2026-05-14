package com.zegao.zeimagessearch.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pexels图片搜索工具
 * 使用Hutool HTTP客户端调用Pexels API进行图片搜索
 */
@Component
@ConfigurationProperties(prefix = "pexels")
public class ImageSearchTool {

    /**
     * Pexels API密钥，从配置文件注入
     * 设计动机：使用外部化配置而非硬编码，支持不同环境使用不同密钥
     */
    private String apiKey;

    private static final String API_BASE_URL = "https://api.pexels.com/v1/search";

    /**
     * 搜索图片
     *
     * @param query   搜索关键词（必填）
     * @param page    页码（可选，默认1）
     * @param perPage 每页数量（可选，默认15，最大80）
     * @return 搜索结果
     */
    @Tool(description = "从Pexels图片库搜索图片，返回匹配的图片信息列表")
    public String searchImages(
            @ToolParam(description = "搜索关键词，例如：nature, city, animal") String query,
            @ToolParam(description = "Page number, default 1", required = false) Integer page,
            @ToolParam(description = "Items per page, default 15, max 80", required = false) Integer perPage) {

        // 参数校验
        if (StrUtil.isBlank(query)) {
            return JSONUtil.toJsonPrettyStr(ApiResponse.error("搜索关键词不能为空"));
        }

        if (StrUtil.isBlank(apiKey)) {
            return JSONUtil.toJsonPrettyStr(ApiResponse.error("Pexels API密钥未配置，请检查配置文件"));
        }

        // 设置默认值
        int currentPage = (page == null || page < 1) ? 1 : page;
        int currentPageSize = (perPage == null || perPage < 1) ? 15 : Math.min(perPage, 80);

        try {
            // 使用Hutool发送HTTP请求
            // 设计动机：Hutool的HttpRequest提供链式调用API，代码更简洁易读
            String response = HttpRequest.get(API_BASE_URL)
                    .header("Authorization", apiKey)
                    .form("query", query)
                    .form("page", currentPage)
                    .form("per_page", currentPageSize)
                    .timeout(10000) // 10秒超时
                    .execute()
                    .body();

            // 解析响应
            JSONObject jsonResponse = JSONUtil.parseObj(response);
            
            // 检查是否有错误
            if (jsonResponse.containsKey("error")) {
                return JSONUtil.toJsonPrettyStr(ApiResponse.error(
                        jsonResponse.getStr("error")));
            }

            // 构建响应对象
            ApiResponse apiResponse = ApiResponse.success(
                    jsonResponse.getInt("total_results"),
                    jsonResponse.getInt("page"),
                    jsonResponse.getInt("per_page"),
                    JSONUtil.toList(jsonResponse.getJSONArray("photos"), Photo.class)
            );

            return JSONUtil.toJsonPrettyStr(apiResponse);

        } catch (Exception e) {
            return JSONUtil.toJsonPrettyStr(ApiResponse.error("图片搜索失败: " + e.getMessage()));
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    // ==================== 数据传输对象 ====================

    /**
     * API统一响应结构
     * 设计动机：统一成功/失败响应格式，便于前端/调用方处理
     */
    @Data
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Integer totalResults;
        private Integer page;
        private Integer perPage;
        private List<Photo> photos;

        public static ApiResponse success(Integer totalResults, Integer page, Integer perPage, List<Photo> photos) {
            ApiResponse response = new ApiResponse();
            response.setSuccess(true);
            response.setMessage("搜索成功");
            response.setTotalResults(totalResults);
            response.setPage(page);
            response.setPerPage(perPage);
            response.setPhotos(photos);
            return response;
        }

        public static ApiResponse error(String message) {
            ApiResponse response = new ApiResponse();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
    }

    /**
     * 图片信息
     * 设计动机：完整映射Pexels API返回的图片结构，包括不同尺寸的图片URL
     */
    @Data
    public static class Photo {
        private Long id;
        private Integer width;
        private Integer height;
        private String url;
        private String photographer;
        private String photographerUrl;
        private Long photographerId;
        private String avgColor;
        private PhotoSrc src;
        private Boolean liked;
        private String alt;
    }

    /**
     * 图片资源URL集合
     * 设计动机：Pexels提供多种尺寸的图片URL，封装为对象便于按需选择合适尺寸
     */
    @Data
    public static class PhotoSrc {
        private String original;   // 原始尺寸
        private String large2x;    // 大尺寸2倍
        private String large;      // 大尺寸
        private String medium;     // 中等尺寸
        private String small;      // 小尺寸
        private String portrait;   // 竖版
        private String landscape;  // 横版
        private String tiny;       // 极小尺寸
    }
}
