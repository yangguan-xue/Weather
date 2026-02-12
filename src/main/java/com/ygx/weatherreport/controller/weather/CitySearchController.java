// controller/city/CitySearchController.java
package com.ygx.weatherreport.controller.weather;

import com.ygx.weatherreport.model.DTO.CitySearchRequestDTO;
import com.ygx.weatherreport.model.DTO.CitySearchResponseDTO;
import com.ygx.weatherreport.service.CitySearchService;
import com.ygx.weatherreport.utils.PinyinUtils;
import com.ygx.weatherreport.utils.ResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/city")
public class CitySearchController {

    @Autowired
    private CitySearchService citySearchService;

    /**
     * 搜索城市（简化版）- 适配您的CitySearchRequestDTO构造函数
     * GET /api/city/search?keyword=北京
     */
    @GetMapping("/search")
    public ResponseWrapper<List<CitySearchResponseDTO.Location>> searchCities(
            @RequestParam @NotBlank(message = "搜索关键词不能为空") String keyword,
            @RequestParam(required = false) String adm,
            @RequestParam(required = false, defaultValue = "cn") String range,
            @RequestParam(required = false, defaultValue = "10") Integer number,
            @RequestParam(required = false, defaultValue = "zh") String lang) {
        try {
            // 详细的参数日志
            log.info("=== 城市搜索请求开始 ===");
            log.info("接收参数: keyword={}, adm={}, range={}, number={}, lang={}",
                    keyword, adm, range, number, lang);

            // 对关键词进行中文到拼音的转换
            String processedKeyword = keyword; // 初始化为原关键词
            if (PinyinUtils.containsChinese(keyword)) {
                String pinyin = PinyinUtils.toPinyin(keyword);
                log.info("检测到中文关键词 '{}'，已转换为拼音：'{}'", keyword, pinyin);
                processedKeyword = pinyin; // 使用拼音作为后续处理的关键词
            }

            // 使用处理后的关键词创建DTO
            CitySearchRequestDTO request = new CitySearchRequestDTO(processedKeyword);

            // 设置可选参数（如果提供了的话）
            if (adm != null && !adm.trim().isEmpty()) {
                // 这里需要反射或setter方法，但您的DTO没有提供setter
                // 所以我们需要使用带所有参数的构造函数，但您的DTO没有
                // 因此，我建议修改DTO或使用其他方式
                log.warn("adm参数被提供但无法设置到DTO: {}", adm);
            }

            // 由于您的构造函数固定了range, number, lang，我们无法修改
            // 但我们可以记录它们是否与默认值不同
            if (!"cn".equals(range)) {
                log.warn("range参数被设置为{}，但DTO构造函数固定为cn", range);
            }
            if (number != 10) {
                log.warn("number参数被设置为{}，但DTO构造函数固定为10", number);
            }
            if (!"zh".equals(lang)) {
                log.warn("lang参数被设置为{}，但DTO构造函数固定为zh", lang);
            }

            // 调用服务
            List<CitySearchResponseDTO.Location> cities = citySearchService.searchCities(request);

            log.info("搜索成功: 找到 {} 个结果", cities.size());
            if (!cities.isEmpty()) {
                log.info("第一个结果: {} (ID: {})",
                        cities.get(0).getName(),
                        cities.get(0).getId());
            }
            log.info("=== 城市搜索请求结束 ===");

            return ResponseWrapper.success(cities);

        } catch (Exception e) {
            log.error("城市搜索异常", e);
            return ResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 搜索城市（完整参数版）- 需要DTO有无参构造函数
     * POST /api/city/search
     *
     * 注意：您的CitySearchRequestDTO没有无参构造函数，这会导致Spring Boot无法反序列化
     * 建议修改DTO添加无参构造函数
     */
    @PostMapping("/search")
    public ResponseWrapper<List<CitySearchResponseDTO.Location>> searchCitiesPost(
            @Valid @RequestBody CitySearchRequestDTO request) {
        try {
            log.info("搜索城市请求(POST): keyword={}", request.getKeyword());

            // 验证必要的字段
            if (request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST.value(),
                        "keyword参数不能为空");
            }

            List<CitySearchResponseDTO.Location> cities = citySearchService.searchCities(request);
            return ResponseWrapper.success(cities);
        } catch (Exception e) {
            log.error("城市搜索异常", e);
            return ResponseWrapper.error(e.getMessage());
        }
    }

    /**
     * 获取热门城市列表
     * GET /api/city/popular
     */
    @GetMapping("/popular")
    public ResponseWrapper<List<CitySearchResponseDTO.Location>> getPopularCities() {
        try {
            log.info("获取热门城市列表");
            List<CitySearchResponseDTO.Location> popularCities = citySearchService.getPopularCities();
            log.info("获取到 {} 个热门城市", popularCities.size());
            return ResponseWrapper.success(popularCities);
        } catch (Exception e) {
            log.error("获取热门城市异常", e);
            return ResponseWrapper.error(e.getMessage());
        }
    }

    /**
     * 根据经纬度搜索城市
     * GET /api/city/search-by-coordinate?lat=39.90498&lon=116.40528
     */
    @GetMapping("/search-by-coordinate")
    public ResponseWrapper<List<CitySearchResponseDTO.Location>> searchByCoordinate(
            @RequestParam @NotBlank(message = "纬度不能为空") String lat,
            @RequestParam @NotBlank(message = "经度不能为空") String lon,
            @RequestParam(required = false, defaultValue = "zh") String lang) {
        try {
            log.info("根据经纬度搜索城市: lat={}, lon={}, lang={}", lat, lon, lang);

            // 验证经纬度格式
            try {
                double latitude = Double.parseDouble(lat);
                double longitude = Double.parseDouble(lon);

                if (latitude < -90 || latitude > 90) {
                    return ResponseWrapper.error(HttpStatus.BAD_REQUEST.value(),
                            "纬度应在-90到90之间");
                }
                if (longitude < -180 || longitude > 180) {
                    return ResponseWrapper.error(HttpStatus.BAD_REQUEST.value(),
                            "经度应在-180到180之间");
                }
            } catch (NumberFormatException e) {
                return ResponseWrapper.error(HttpStatus.BAD_REQUEST.value(),
                        "经纬度格式不正确，应为数字");
            }

            // 构造坐标字符串
            String coordinate = lat + "," + lon;
            CitySearchRequestDTO request = new CitySearchRequestDTO(coordinate);

            List<CitySearchResponseDTO.Location> cities = citySearchService.searchCities(request);
            return ResponseWrapper.success(cities);

        } catch (Exception e) {
            log.error("根据经纬度搜索城市异常", e);
            return ResponseWrapper.error(e.getMessage());
        }
    }

    /**
     * 健康检查端点
     * GET /api/city/health
     */
    @GetMapping("/health")
    public ResponseWrapper<Object> healthCheck() {
        try {
            log.info("城市搜索服务健康检查");
            return ResponseWrapper.success("服务正常");
        } catch (Exception e) {
            log.error("健康检查异常", e);
            return ResponseWrapper.error("服务异常: " + e.getMessage());
        }
    }

    /**
     * 编码诊断端点
     * GET /api/city/debug/encoding?input=北京
     */
    @GetMapping("/debug/encoding")
    public ResponseWrapper<Object> debugEncoding(@RequestParam String input) {
        try {
            log.info("编码诊断请求: input={}", input);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("originalInput", input);
            result.put("length", input.length());

            // 获取字节信息
            try {
                byte[] utf8Bytes = input.getBytes("UTF-8");
                result.put("utf8BytesHex", bytesToHex(utf8Bytes));
                result.put("utf8BytesLength", utf8Bytes.length);
            } catch (Exception e) {
                result.put("utf8BytesError", e.getMessage());
            }

            // URL编码
            try {
                String urlEncoded = java.net.URLEncoder.encode(input, "UTF-8");
                result.put("urlEncoded", urlEncoded);
            } catch (Exception e) {
                result.put("urlEncodingError", e.getMessage());
            }

            return ResponseWrapper.success(result);

        } catch (Exception e) {
            log.error("编码诊断异常", e);
            return ResponseWrapper.error(e.getMessage());
        }
    }

    /**
     * 字节转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X ", b));
        }
        return hex.toString().trim();
    }
}