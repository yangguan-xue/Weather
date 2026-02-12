// model/DTO/CitySearchRequestDTO.java
package com.ygx.weatherreport.model.DTO;

import lombok.Data;

@Data
public class CitySearchRequestDTO {
    private String keyword;     // 搜索关键字
    private String adm;         // 上级行政区划（可选）
    private String range;       // 搜索范围（可选，默认cn）
    private Integer number;     // 返回数量（可选，默认10）
    private String lang;        // 语言（可选，默认zh）

    public CitySearchRequestDTO() {
        this.range = "cn";
        this.number = 10;
        this.lang = "zh";
    }

    public CitySearchRequestDTO(String keyword) {
        this.keyword = keyword;
        this.range = "cn";
        this.number = 10;
        this.lang = "zh";
    }
}
