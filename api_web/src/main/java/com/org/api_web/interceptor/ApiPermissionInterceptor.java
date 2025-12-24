package com.org.api_web.interceptor;

import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_common.entity.SysUser;
import com.org.api_common.exception.BusinessException;
import com.org.api_admin_service.service.ApiInfoService;
import com.org.api_admin_service.service.ApiPermissionService;
import com.org.api_service.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务接口权限拦截器
 * 拦截业务接口调用，校验用户是否有该接口的调用权限
 */
@Component
public class ApiPermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ApiInfoService apiInfoService;

    @Autowired
    private ApiPermissionService apiPermissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // 1. 从请求参数中获取签名参数（业务接口通过AK/SK调用）
        String accessKey = request.getParameter("accessKey");
        String sign = request.getParameter("sign");
        String timestampStr = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");

        if (!StringUtils.hasText(accessKey)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "缺少accessKey参数");
        }

        // 2. 先校验AK/SK签名（必须先校验签名，确保请求合法）
        if (StringUtils.hasText(sign) && StringUtils.hasText(timestampStr) && StringUtils.hasText(nonce)) {
            try {
                Long timestamp = Long.parseLong(timestampStr);
                sysUserService.verifyAkSk(accessKey, sign, timestamp, nonce);
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "时间戳格式错误");
            }
        } else {
            // 如果没有签名参数，可能是其他类型的请求，放行
            return true;
        }

        // 3. 根据accessKey获取用户信息
        SysUser user = sysUserService.getByAccessKey(accessKey);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "用户不存在或AK已禁用");
        }

        // 4. 根据请求路径和方法查询接口信息
        com.org.api_common.entity.ApiInfo apiInfo = apiInfoService.getValidApiByPathAndMethod(requestPath, method);
        if (apiInfo == null) {
            // 如果接口不存在，可能是非业务接口，放行（由其他拦截器处理）
            return true;
        }

        // 5. 查询用户是否有该接口的权限
        List<com.org.api_common.vo.ApiPermissionVO> permissions = apiPermissionService.getUserApiPermissions(user.getId());
        
        boolean hasPermission = permissions.stream()
                .anyMatch(p -> p.getApiId().equals(apiInfo.getId())
                        && p.getStatus() == 1
                        && (p.getExpireTime() == null || p.getExpireTime().isAfter(LocalDateTime.now())));

        if (!hasPermission) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENY, 
                    "接口权限不足，请先开通该接口的调用权限。接口：" + apiInfo.getApiName());
        }

        return true;
    }
}

