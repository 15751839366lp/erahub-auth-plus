package com.erahub.resource.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erahub.common.core.constant.CacheNames;
import com.erahub.common.core.exception.ServiceException;
import com.erahub.common.core.utils.StringUtils;
import com.erahub.common.mybatis.core.page.PageQuery;
import com.erahub.common.mybatis.core.page.TableDataInfo;
import com.erahub.common.oss.core.OssClient;
import com.erahub.common.oss.entity.UploadResult;
import com.erahub.common.oss.factory.OssFactory;
import com.erahub.resource.domain.SysOss;
import com.erahub.resource.domain.bo.SysOssBo;
import com.erahub.resource.domain.vo.SysOssVo;
import com.erahub.resource.mapper.SysOssMapper;
import com.erahub.resource.service.ISysOssService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 文件上传 服务层实现
 *
 * @author erahub
 */
@RequiredArgsConstructor
@Service
public class SysOssServiceImpl implements ISysOssService {

    private final SysOssMapper baseMapper;

    @Override
    public TableDataInfo<SysOssVo> queryPageList(SysOssBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysOss> lqw = buildQueryWrapper(bo);
        Page<SysOssVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<SysOssVo> listByIds(Collection<Long> ossIds) {
        List<SysOssVo> list = new ArrayList<>();
        for (Long id : ossIds) {
            SysOssVo vo = getById(id);
            if (ObjectUtil.isNotNull(vo)) {
                list.add(vo);
            }
        }
        return list;
    }

    private LambdaQueryWrapper<SysOss> buildQueryWrapper(SysOssBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysOss> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getFileName()), SysOss::getFileName, bo.getFileName());
        lqw.like(StringUtils.isNotBlank(bo.getOriginalName()), SysOss::getOriginalName, bo.getOriginalName());
        lqw.eq(StringUtils.isNotBlank(bo.getFileSuffix()), SysOss::getFileSuffix, bo.getFileSuffix());
        lqw.eq(StringUtils.isNotBlank(bo.getUrl()), SysOss::getUrl, bo.getUrl());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            SysOss::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.eq(StringUtils.isNotBlank(bo.getCreateBy()), SysOss::getCreateBy, bo.getCreateBy());
        lqw.eq(StringUtils.isNotBlank(bo.getService()), SysOss::getService, bo.getService());
        return lqw;
    }

    @Cacheable(cacheNames = CacheNames.SYS_OSS, key = "#ossId")
    @Override
    public SysOssVo getById(Long ossId) {
        return baseMapper.selectVoById(ossId);
    }

    @Override
    public SysOss upload(MultipartFile file) {
        String originalfileName = file.getOriginalFilename();
        String suffix = StringUtils.substring(originalfileName, originalfileName.lastIndexOf("."), originalfileName.length());
        OssClient storage = OssFactory.instance();
        UploadResult uploadResult;
        try {
            uploadResult = storage.uploadSuffix(file.getBytes(), suffix, file.getContentType());
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
        // 保存文件信息
        SysOss oss = new SysOss();
        oss.setUrl(uploadResult.getUrl());
        oss.setFileSuffix(suffix);
        oss.setFileName(uploadResult.getFilename());
        oss.setOriginalName(originalfileName);
        oss.setService(storage.getConfigKey());
        baseMapper.insert(oss);
        return oss;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 做一些业务上的校验,判断是否需要校验
        }
        List<SysOss> list = baseMapper.selectBatchIds(ids);
        for (SysOss sysOss : list) {
            OssClient storage = OssFactory.instance(sysOss.getService());
            storage.delete(sysOss.getUrl());
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

}
