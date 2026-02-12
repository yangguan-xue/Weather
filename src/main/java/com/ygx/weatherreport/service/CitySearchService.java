// service/CitySearchService.java
package com.ygx.weatherreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ygx.weatherreport.model.DTO.CitySearchRequestDTO;
import com.ygx.weatherreport.model.DTO.CitySearchResponseDTO;
import com.ygx.weatherreport.utils.QWeatherJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;


@Slf4j
@Service
public class CitySearchService {

    @Value("${qweather.api-host}")
    private String apiHost;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QWeatherJwtUtil qWeatherJwtUtil; // 注入JWT工具

    /**
     * 搜索城市
     */
    public List<CitySearchResponseDTO.Location> searchCities(CitySearchRequestDTO request) {
        validateRequest(request);
        try {
            // 1. 生成JWT Token
            String jwtToken = qWeatherJwtUtil.generateQWeatherToken();

            // 2. 构建API请求URL (不再需要key查询参数)
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiHost)
                    .path("/geo/v2/city/lookup")
                    .queryParam("location", request.getKeyword());

            if (request.getAdm() != null && !request.getAdm().trim().isEmpty()) {
                builder.queryParam("adm", request.getAdm());
            }
            if (request.getRange() != null && !request.getRange().trim().isEmpty()) {
                builder.queryParam("range", request.getRange());
            }
            if (request.getNumber() != null && request.getNumber() > 0) {
                builder.queryParam("number", Math.min(request.getNumber(), 20));
            }
            if (request.getLang() != null && !request.getLang().trim().isEmpty()) {
                builder.queryParam("lang", request.getLang());
            }

            String url = builder.toUriString();
            log.info("调用和风天气城市搜索API: {}", url);

            // 3. 设置请求头，使用Bearer Token认证
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Accept-Encoding", "gzip, deflate");
            headers.set("Authorization", "Bearer " + jwtToken); // 关键修改

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 4. 调用API
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("城市搜索API请求失败: " + response.getStatusCode());
            }

            // 5. 处理响应 (GZIP解压和JSON解析，代码与您原文档完全一致)
            String jsonResponse = processGzipResponse(response.getHeaders(), response.getBody());
            log.debug("城市搜索API响应: {}", jsonResponse);

            CitySearchResponseDTO responseDTO = objectMapper.readValue(
                    jsonResponse, CitySearchResponseDTO.class);

            if (!"200".equals(responseDTO.getCode())) {
                throw new RuntimeException("城市搜索API错误: code=" + responseDTO.getCode());
            }

            List<CitySearchResponseDTO.Location> locations = responseDTO.getLocation();
            if (locations == null || locations.isEmpty()) {
                log.info("未找到匹配的城市: keyword={}", request.getKeyword());
                return List.of();
            }

            log.info("找到{}个匹配城市: keyword={}", locations.size(), request.getKeyword());
            return locations;

        } catch (Exception e) {
            log.error("城市搜索失败: keyword={}", request.getKeyword(), e);
            throw new RuntimeException("城市搜索失败: " + e.getMessage());
        }
    }

    /**
     * 简化搜索方法
     * @param keyword 搜索关键字
     * @return 城市搜索结果
     */
    public List<CitySearchResponseDTO.Location> searchCities(String keyword) {
        CitySearchRequestDTO request = new CitySearchRequestDTO(keyword);
        return searchCities(request);
    }

    /**
     * 验证请求参数
     */
    private void validateRequest(CitySearchRequestDTO request) {
        if (request == null || request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键字不能为空");
        }

        if (request.getKeyword().trim().length() < 1) {
            throw new IllegalArgumentException("搜索关键字至少需要1个字符");
        }

        if (request.getNumber() != null && (request.getNumber() < 1 || request.getNumber() > 20)) {
            throw new IllegalArgumentException("返回数量必须在1-20之间");
        }
    }

    /**
     * 处理GZIP响应
     */
    private String processGzipResponse(HttpHeaders headers, byte[] responseBody) throws Exception {
        if (responseBody == null) {
            return "";
        }

        // 检查是否为GZIP压缩
        String contentEncoding = headers.getFirst("Content-Encoding");
        boolean isGzip = (contentEncoding != null && contentEncoding.contains("gzip"))
                || isGzipped(responseBody);

        if (isGzip) {
            return decompressGzip(responseBody);
        } else {
            return new String(responseBody, "UTF-8");
        }
    }

    /**
     * 检查是否为GZIP格式
     */
    private boolean isGzipped(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }
        // GZIP魔数：0x1F 0x8B
        return (data[0] == (byte) 0x1F && data[1] == (byte) 0x8B);
    }

    /**
     * 解压GZIP数据
     */
    private String decompressGzip(byte[] compressedData) throws Exception {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
             GZIPInputStream gis = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toString("UTF-8");
        }
    }

    /**
     * 获取热门城市列表
     */
    public List<CitySearchResponseDTO.Location> getPopularCities() {
        // 这里可以返回预设的热门城市
        // 或者从配置文件/数据库中读取
        return List.of(
                createLocation("北京", "101010100", "39.90498", "116.40528", "北京", "北京市", "中国"),
                createLocation("上海", "101020100", "31.23171", "121.47264", "上海", "上海市", "中国"),
                createLocation("广州", "101280101", "23.12518", "113.28064", "广州", "广东省", "中国"),
                createLocation("深圳", "101280601", "22.54700", "114.08595", "深圳", "广东省", "中国"),
                createLocation("杭州", "101210101", "30.28746", "120.15358", "杭州", "浙江省", "中国"),
                createLocation("南京", "101190101", "32.04154", "118.76741", "南京", "江苏省", "中国"),
                createLocation("成都", "101270101", "30.65946", "104.06573", "成都", "四川省", "中国"),
                createLocation("武汉", "101200101", "30.58435", "114.29857", "武汉", "湖北省", "中国"),
                createLocation("西安", "101110101", "34.26316", "108.94802", "西安", "陕西省", "中国"),
                createLocation("重庆", "101040100", "29.56376", "106.55046", "重庆", "重庆市", "中国")
        );
    }

    private CitySearchResponseDTO.Location createLocation(
            String name, String id, String lat, String lon,
            String adm2, String adm1, String country) {
        CitySearchResponseDTO.Location location = new CitySearchResponseDTO.Location();
        location.setName(name);
        location.setId(id);
        location.setLat(lat);
        location.setLon(lon);
        location.setAdm2(adm2);
        location.setAdm1(adm1);
        location.setCountry(country);
        location.setTz("Asia/Shanghai");
        location.setUtcOffset("+08:00");
        location.setIsDst("0");
        location.setType("city");
        location.setRank("10");
        location.setFxLink(String.format("https://www.qweather.com/weather/%s-%s.html",
                name.toLowerCase(), id));
        return location;
    }
}
