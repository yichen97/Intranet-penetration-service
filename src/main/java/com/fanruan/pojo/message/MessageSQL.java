package com.fanruan.pojo.message;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageSQL {
    private String dBName;
    private String SQL;
}
