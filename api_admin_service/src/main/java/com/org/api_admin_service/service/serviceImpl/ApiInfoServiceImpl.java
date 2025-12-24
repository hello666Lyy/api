package com.org.api_admin_service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.org.api_admin_service.mapper.ApiInfoMapper;
import com.org.api_admin_service.service.ApiInfoService;
import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_common.entity.ApiInfo;
import com.org.api_common.exception.BusinessException;
import com.org.api_common.dto.ApiInfoDTO;
import com.org.api_common.vo.ApiInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 接口信息业务实现
 */
@Service
public class ApiInfoServiceImpl extends ServiceImpl<ApiInfoMapper, ApiInfo>
        implements ApiInfoService {

    @Override
    public ApiInfo getValidApiByPathAndMethod(String apiPath, String method) {
        LambdaQueryWrapper<ApiInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiInfo::getApiPath, apiPath)
                .eq(ApiInfo::getMethod, method.toUpperCase())
                .eq(ApiInfo::getStatus, 1);
        return this.getOne(wrapper);
    }

    @Override
    public void checkApiAvailable(String apiPath, String method) {
        ApiInfo apiInfo = this.getValidApiByPathAndMethod(apiPath, method);
        if (apiInfo == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND,
                    "接口不存在或已禁用: " + method + " " + apiPath);
        }
    }

    @Override
    public ApiInfoVO addApi(ApiInfoDTO apiInfoDTO) {
        // 1. 校验接口路径+方法是否已存在
        this.checkApiPathUnique(apiInfoDTO.getApiPath(), apiInfoDTO.getMethod(), null);

        // 2. 创建接口信息
        ApiInfo apiInfo = new ApiInfo();
        BeanUtils.copyProperties(apiInfoDTO, apiInfo);
        apiInfo.setMethod(apiInfoDTO.getMethod().toUpperCase());
        apiInfo.setCreateTime(LocalDateTime.now());
        apiInfo.setUpdateTime(LocalDateTime.now());

        apiInfo.setVersion(0); // 新增：初始化乐观锁版本号

        // 3. 保存
        this.save(apiInfo);

        // 4. 转换为VO返回
        return convertToVO(apiInfo);
    }

    @Override
    public ApiInfoVO updateApi(Long id, ApiInfoDTO apiInfoDTO) {
        // 1. 查询接口是否存在
        ApiInfo apiInfo = this.getById(id);
        if (apiInfo == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "接口不存在");
        }

        // 2. 校验接口路径+方法是否与其他接口冲突
        this.checkApiPathUnique(apiInfoDTO.getApiPath(), apiInfoDTO.getMethod(), id);

        // 3. 更新信息
        BeanUtils.copyProperties(apiInfoDTO, apiInfo);
        apiInfo.setMethod(apiInfoDTO.getMethod().toUpperCase());
        apiInfo.setUpdateTime(LocalDateTime.now());

//        // 3. 故意卡住 15 秒，制造“别人来抢”的时间窗口
//        try {
//            Thread.sleep(8_000); // 休眠 15 秒
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }

        // 4. 保存（乐观锁生效）
        boolean success = this.updateById(apiInfo); // 新增：接收返回值
        if (!success) { // 新增：并发失败判断
            throw new BusinessException(
                    ErrorCodeEnum.SYSTEM_ERROR,
                    "该接口已被其他管理员修改，请刷新后重试"
            );
        }

        // 5. 返回更新后的信息
        return convertToVO(apiInfo);
    }

    @Override
    public void deleteApi(Long id) {
        // 逻辑删除：将状态设为禁用
        ApiInfo apiInfo = this.getById(id);
        if (apiInfo == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "接口不存在");
        }

        apiInfo.setStatus(0);
        apiInfo.setUpdateTime(LocalDateTime.now());

//        // 3. 故意卡住 15 秒，制造“别人来抢”的时间窗口
//        try {
//            Thread.sleep(8_000); // 休眠 15 秒
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }

        boolean success = this.updateById(apiInfo); // 新增：用返回值判断
        if (!success) { // 新增：并发失败判断
            throw new BusinessException(
                    ErrorCodeEnum.SYSTEM_ERROR,
                    "该接口已被其他管理员修改或删除，请刷新后重试"
            );
        }
    }

    @Override
    public IPage<ApiInfoVO> pageApiList(Integer pageNum, Integer pageSize,
                                        String apiName, String apiPath, Integer status) {
        // 1. 构建分页对象
        Page<ApiInfo> page = new Page<>(pageNum != null ? pageNum : 1,
                pageSize != null ? pageSize : 10);

        // 2. 构建查询条件
        LambdaQueryWrapper<ApiInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(apiName)) {
            wrapper.like(ApiInfo::getApiName, apiName);
        }
        if (StringUtils.hasText(apiPath)) {
            wrapper.like(ApiInfo::getApiPath, apiPath);
        }
        if (status != null) {
            wrapper.eq(ApiInfo::getStatus, status);
        }
        wrapper.orderByDesc(ApiInfo::getCreateTime);

        // 3. 执行分页查询
        IPage<ApiInfo> apiPage = this.page(page, wrapper);

        // 4. 转换为VO
        List<ApiInfoVO> voList = apiPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 5. 构建返回结果
        IPage<ApiInfoVO> voPage = new Page<>(apiPage.getCurrent(), apiPage.getSize(), apiPage.getTotal());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public ApiInfoVO getApiById(Long id) {
        ApiInfo apiInfo = this.getById(id);
        if (apiInfo == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "接口不存在");
        }
        return convertToVO(apiInfo);
    }

    @Override
    public ApiInfoVO updateApiStatus(Long id, Integer status) {
        // 1. 参数校验
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "状态值只能是0或1");
        }

        // 2. 查询接口
        ApiInfo apiInfo = this.getById(id);
        if (apiInfo == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "接口不存在");
        }

        // 3. 更新状态
        apiInfo.setStatus(status);
        apiInfo.setUpdateTime(LocalDateTime.now());

//        // 3. 故意卡住 15 秒，制造“别人来抢”的时间窗口
//        try {
//            Thread.sleep(8_000); // 休眠 15 秒
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }

        // 4. 保存（乐观锁）
        boolean success = this.updateById(apiInfo); // 新增：用返回值判断
        if (!success) { // 新增：并发失败判断
            throw new BusinessException(
                    ErrorCodeEnum.SYSTEM_ERROR,
                    "该接口状态已被其他管理员修改，请刷新后重试"
            );
        }

        return convertToVO(apiInfo);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 校验接口路径+方法的唯一性
     */
    private void checkApiPathUnique(String apiPath, String method, Long excludeId) {
        LambdaQueryWrapper<ApiInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiInfo::getApiPath, apiPath)
                .eq(ApiInfo::getMethod, method.toUpperCase());
        if (excludeId != null) {
            wrapper.ne(ApiInfo::getId, excludeId);
        }

        ApiInfo existing = this.getOne(wrapper);
        if (existing != null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,
                    "接口路径+方法已存在: " + method.toUpperCase() + " " + apiPath);
        }
    }

    /**
     * 实体转换为VO
     */
    private ApiInfoVO convertToVO(ApiInfo apiInfo) {
        ApiInfoVO vo = new ApiInfoVO();
        BeanUtils.copyProperties(apiInfo, vo);
        return vo;
    }
}