package com.fanruan.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 封装数据源信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyDataSource {
    private String DataSourceName;
    private String URL;
    private String userName;
    private String passWord;
}
