package com.erahub.resource.api.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件信息
 *
 * @author erahub
 */
@Data
public class SysFile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件地址
     */
    private String url;

}
