// model/DTO/CitySearchResponseDTO.java
package com.ygx.weatherreport.model.DTO;

import lombok.Data;
import java.util.List;

@Data
public class CitySearchResponseDTO {
    private String code;
    private String updateTime;
    private List<Location> location;
    private Refer refer;

    @Data
    public static class Location {
        private String name;        // 地区/城市名称
        private String id;          // 地区/城市ID
        private String lat;         // 地区/城市纬度
        private String lon;         // 地区/城市经度
        private String adm2;        // 地区/城市的上级行政区划名称
        private String adm1;        // 地区/城市所属一级行政区域
        private String country;     // 地区/城市所属国家名称
        private String tz;          // 地区/城市所在时区
        private String utcOffset;   // 地区/城市目前与UTC时间偏移的小时数
        private String isDst;       // 地区/城市是否当前处于夏令时
        private String type;        // 地区/城市的属性
        private String rank;        // 地区评分
        private String fxLink;      // 该地区的天气预报网页链接
    }

    @Data
    public static class Refer {
        private List<String> sources;
        private List<String> license;
    }
}
