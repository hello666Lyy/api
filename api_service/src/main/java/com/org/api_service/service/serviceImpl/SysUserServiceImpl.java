package com.org.api_service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.org.api_common.constant.AkOperateTypeEnum;
import com.org.api_common.constant.AkPermissionTypeEnum;
import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_common.constant.SignConstants;
import com.org.api_common.entity.AkOperateLog;
import com.org.api_common.entity.SysUser;
import com.org.api_common.exception.BusinessException;
import com.org.api_common.util.NonceUtil;
import com.org.api_common.util.SecretKeyUtil;
import com.org.api_common.util.SignUtil;
import com.org.api_common.vo.*;
import com.org.api_service.mapper.SysUserMapper;
import com.org.api_service.service.AkOperateLogService;
import com.org.api_service.service.SysUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 用户业务实现类（AK/SK校验核心）
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    // 随机字符池（生成AK/SK用）
    private static final String RANDOM_CHAR_POOL = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final Random random = new Random();
    // ========== 补充这几个缺失的静态常量 ==========
    // 安全随机数生成器（避免伪随机，保证密钥安全性）
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    // 密钥字符集（数字+大小写字母，保证复杂度）
    private static final String KEY_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    // 密钥长度（32位，符合行业规范）
    private static final int SECRET_KEY_LENGTH = 32;
    // ==========================================

    @Autowired
    private AkOperateLogService  akOperateLogService;

    // 新增方法：生成新AK/SK
    @Override
    public Map<String, String> generateNewAkSk(String accessKey) {
        // 1. 查询当前用户
        SysUser sysUser = this.getByAccessKey(accessKey);
        if (sysUser == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "用户不存在");
        }

        // 2. 生成唯一AK（16位，确保数据库中无重复）
        String newAk;
        do {
            newAk = generateRandomStr(SignConstants.AK_LENGTH);
        } while (baseMapper.selectByAccessKey(newAk) != null); // 校验AK唯一性

        // 3. 生成SK（32位，随机字符串）
        String newSk = generateRandomStr(SignConstants.SK_LENGTH);

        // 4. 更新用户的AK/SK
        sysUser.setAccessKey(newAk);
        sysUser.setSecretKey(newSk);
        this.updateById(sysUser);

        // 5. 返回新AK/SK（SK仅返回一次）
        Map<String, String> result = new HashMap<>();
        result.put("newAccessKey", newAk);
        result.put("newSecretKey", newSk);
        return result;
    }

    // 辅助方法：生成指定长度的随机字符串
    private String generateRandomStr(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHAR_POOL.charAt(random.nextInt(RANDOM_CHAR_POOL.length())));
        }
        return sb.toString();
    }
    // 根据ak 查SysUser 一个时鉴权， 另一个是所有状态都可以查（status = 0 and 1 )
    public SysUser getByAccessKey(String accessKey) {
        // 1. 参数非空校验
        if (!StringUtils.hasText(accessKey)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "AK不能为空");
        }
        // 2. 查询有效用户
        SysUser user = baseMapper.selectByAccessKey(accessKey);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "AK不存在或用户已禁用");
        }
        return user;
    }
    public SysUser getByAccessKeyAllStatus(String accessKey) {
        // 参数非空校验
        if (!StringUtils.hasText(accessKey)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "AK不能为空");
        }
        return baseMapper.selectByAccessKeyAllStatus(accessKey);
    }

    @Override
    public boolean verifyAkSk(String accessKey, String sign, Long timestamp, String nonce) {
        // ===== 新增调试日志（仅打印，不改动原有逻辑）=====
        System.out.println("===== 签名校验调试日志 =====");
        System.out.println("1. 入参：accessKey=" + accessKey + ", sign=" + sign + ", timestamp=" + timestamp + ", nonce=" + nonce);
        // ===== 原有代码（一行未改）=====
        System.out.println("服务器当前时间戳（毫秒）：" + System.currentTimeMillis());
        System.out.println("你传的时间戳：" + timestamp);
        // 1. 基础参数校验
        if (!StringUtils.hasText(sign) || timestamp == null || !StringUtils.hasText(nonce)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "签名/时间戳/Nonce不能为空");
        }
//         2. 时间戳过期校验（有效期5分钟）
        if (!SignUtil.verifyTimestamp(timestamp)) {
            throw new BusinessException(ErrorCodeEnum.TIMESTAMP_TIMEOUT, "时间戳已过期（有效期5分钟）");
        }
        // 3. Nonce重复校验（防止重放攻击）
        if (!NonceUtil.verifyNonce(accessKey, nonce)) {
            throw new BusinessException(ErrorCodeEnum.NONCE_REPEAT, "请求重复（Nonce已使用）");
        }
        // 4. 查询用户SK
        SysUser user = this.getByAccessKey(accessKey);

        // ===== 新增调试日志（仅打印，不改动原有逻辑）=====
        System.out.println("2. 查询到的用户SK：" + user.getSecretKey());
        // 5. 签名校验（核心）
        Map<String, Object> params = new HashMap<>();
        params.put("accessKey", accessKey);
        params.put("timestamp", timestamp);
        params.put("nonce", nonce);
        boolean signValid = SignUtil.verifySign(params, user.getSecretKey(), sign);
        if (!signValid) {
            throw new BusinessException(ErrorCodeEnum.SIGN_ERROR, "签名验证失败");
        }
        return true;
    }

    /**
     *
     * @param accessKey
     * @return
     */
    @Override
    public UserInfoVO getUserInfoVO(String accessKey) {
        // 1. 调用原有方法查询用户
        SysUser sysUser = this.getByAccessKey(accessKey);
        // 2. 转换为VO（隐藏SK）
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(sysUser, userInfoVO);
        return userInfoVO;
    }

    @Override
    public boolean updateAkStatus(String accessKey, Integer status) {
        // 1. 校验状态参数合法性
        if (status == null || (status != SignConstants.AK_STATUS_ENABLE && status != SignConstants.AK_STATUS_DISABLE)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "状态值仅支持1（启用）或0（禁用）");
        }
        // 2. 查询用户
        SysUser sysUser = this.getByAccessKey(accessKey);
        if (sysUser == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "AK不存在或已禁用");
        }
        // 3. 更新状态
        sysUser.setStatus(status);
        boolean updateResult = this.updateById(sysUser);
        if (!updateResult) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "修改AK状态失败");
        }
        return true;
    }

    @Override
    public AkStatusVO getAkStatus(String accessKey) {
        // 1. 查询用户（使用AllStatus方法，可以查询到禁用状态的AK）
        SysUser sysUser = this.getByAccessKeyAllStatus(accessKey);
        if (sysUser == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "AK不存在");
        }
        // 2. 转换为VO返回（包含状态、更新时间等核心信息）
        AkStatusVO akStatusVO = new AkStatusVO();
        BeanUtils.copyProperties(sysUser, akStatusVO);
        return akStatusVO;
    }

    // 新增：批量查询AK状态核心逻辑
    @Override
    public BatchAkStatusVO batchGetAkStatus(String akListStr) {
        // 1. 校验入参：AK列表不能为空
        if (!StringUtils.hasText(akListStr)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "AK列表不能为空（多个AK用逗号分隔）");
        }

        // 2. 拆分AK列表（逗号分隔，去除每个AK的空格）
        String[] akArray = akListStr.split(",");
        List<String> akList = new ArrayList<>();
        for (String ak : akArray) {
            String trimAk = ak.trim();
            if (StringUtils.hasText(trimAk)) { // 过滤空字符串
                akList.add(trimAk);
            }
        }

        // 3. 初始化返回VO
        BatchAkStatusVO batchVO = new BatchAkStatusVO();
        batchVO.setTotal(akList.size());
        List<SingleAkStatusVO> singleList = new ArrayList<>();
        int existCount = 0;

        // 4. 循环查询每个AK的状态
        for (String ak : akList) {
            SingleAkStatusVO singleVO = new SingleAkStatusVO();
            singleVO.setAccessKey(ak);

            // 查询AK（注意：这里查所有状态的AK，包括禁用，仅鉴权时才过滤status=1）
            SysUser sysUser = baseMapper.selectByAccessKey(ak);
            if (sysUser != null) {
                singleVO.setExist(true);
                singleVO.setStatus(sysUser.getStatus());
                singleVO.setUpdateTime(sysUser.getUpdateTime()); // 已统一为LocalDateTime
                existCount++;
            } else {
                singleVO.setExist(false);
                singleVO.setStatus(null);
                singleVO.setUpdateTime(null);
            }
            singleList.add(singleVO);
        }

        // 5. 封装最终结果
        batchVO.setExistCount(existCount);
        batchVO.setAkStatusList(singleList);
        return batchVO;
    }

    @Override
    public AkExpireTimeVO manageAkExpireTime(String targetAk, LocalDateTime expireTime) {
        // 原逻辑：SysUser sysUser = this.getByAccessKey(targetAk); // 会过滤禁用AK，导致报错
        // 新逻辑：查询所有状态的AK
        SysUser sysUser = this.getByAccessKeyAllStatus(targetAk);
        if (sysUser == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "目标AK不存在");
        }

        // 后续的过期时间判断、设置逻辑（完全保留不变）
        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = false;
        String expireDesc = "";

        if (expireTime != null) {
            // 1. 设置新的过期时间
            sysUser.setExpireTime(expireTime);
            // 2. 初始化过期状态和描述（避免重复定义）
            isExpired = expireTime.isBefore(now);

            // 3. 合并逻辑：判断是否过期 → 无需后续重复计算
            if (isExpired) {
                sysUser.setStatus(SignConstants.AK_STATUS_DISABLE);
                expireDesc = "已过期（已自动禁用）";
            } else {
                // 直接用当前设置的expireTime计算剩余天数（无需再从sysUser获取，减少冗余）
                long days = java.time.temporal.ChronoUnit.DAYS.between(now, expireTime);
                expireDesc = "未过期(剩余" + days + "天)";
            }

            this.updateById(sysUser);

        } else {
            LocalDateTime userExpireTime = sysUser.getExpireTime();
            if (userExpireTime == null) {
                expireDesc = "永不过期";
            } else {
                isExpired = userExpireTime.isBefore(now);
                if (isExpired) {
                    expireDesc = "已过期";
                } else {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(now, userExpireTime);
                    expireDesc = "未过期（剩余" + days + "天）";
                }
            }
        }

        AkExpireTimeVO vo = new AkExpireTimeVO();
        vo.setTargetAk(targetAk);
        vo.setExpireTime(sysUser.getExpireTime());
        vo.setIsExpired(isExpired);
        vo.setExpireDesc(expireDesc);
        return vo;
    }

    /**
     * 权限管理核心逻辑（注释：无冗余，步骤清晰）
     */
    @Override
    public AkPermissionVO manageAkPermission(String targetAk, Integer permissionType) {
        // 1. 查询所有状态的AK（注释：兼容禁用AK的权限查询/分配）
        SysUser sysUser = this.getByAccessKeyAllStatus(targetAk);
        if (sysUser == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "目标AK不存在");
        }

        // 2. 权限分配逻辑（注释：仅传permissionType时执行，无重复操作）
        if (permissionType != null) {
            // 前置校验：权限类型合法性（注释：避免非法值入库）
            if (permissionType < 1 || permissionType > 3) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "权限类型非法（仅支持1=只读、2=读写、3=管理员）");
            }
            // 设置权限并更新（注释：仅执行一次更新，无冗余）
            sysUser.setPermissionType(permissionType);
            this.updateById(sysUser);
        }

        // 3. 封装返回VO（注释：转换编码为中文描述，前端无需二次处理）
        AkPermissionVO vo = new AkPermissionVO();
        vo.setTargetAk(targetAk);
        vo.setPermissionType(sysUser.getPermissionType());
        vo.setPermissionDesc(AkPermissionTypeEnum.getDescByCode(sysUser.getPermissionType()));

        return vo;
    }

    // 1.2 SysUserServiceImpl实现（注释：逻辑无冗余，一次查询+一次更新+日志记录）
    @Override
    public String resetAkSecretKey(String targetAk) {
        // 1. 查询所有状态的AK（兼容禁用AK，但需存在）
        SysUser sysUser = this.getByAccessKeyAllStatus(targetAk);
        if (sysUser == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "资源不存在：目标AK不存在");
        }

        // 2. 安全校验：禁止重置管理员AK（避免管理员自己被锁死）
        if (AkPermissionTypeEnum.ADMIN.getCode().equals(sysUser.getPermissionType())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN, "禁止操作：管理员AK不允许重置密钥");
        }

        // 3. 生成新的安全secret_key（工具类生成）
        String newSecretKey = SecretKeyUtil.generateRandomSecretKey();

        // 4. 更新数据库（仅更新secret_key字段）
        sysUser.setSecretKey(newSecretKey);
        this.updateById(sysUser);

        // 5. 记录重置日志（便于审计）
        AkOperateLog log = new AkOperateLog();
        log.setTargetAk(targetAk);
        log.setOperateType(AkOperateTypeEnum.RESET_SECRET_KEY.getCode()); // 新增枚举值：4=重置密钥
        log.setOperator("admin"); // 实际应为当前管理员AK，这里简化
        log.setRemark("管理员重置AK密钥，新密钥：" + newSecretKey); // 生产环境建议隐藏密钥后几位
        akOperateLogService.save(log);

        // 6. 返回新密钥（仅返回一次，调用方需妥善保存）
        return newSecretKey;
    }

    /**
     * 批量操作AK状态（启用/禁用），加事务保证批量操作原子性
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 事务保留，失败全回滚
    public int batchOperateAkStatus(Integer operateType, String targetAks, String remark) {
        // 1. 参数校验（仅校验2/3，匹配枚举ENABLE_AK/DISABLE_AK，无其他变动）
        if (operateType == null || (operateType != 2 && operateType != 3)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "操作类型非法！仅支持2=启用AK/3=禁用AK（匹配枚举/数据库）");
        }
        if (!StringUtils.hasText(targetAks)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "批量AK列表不能为空！");
        }

        // 2. 拆分AK列表（无变动）
        List<String> akList = Arrays.asList(targetAks.split(","));
        if (akList.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "批量AK格式错误！需逗号分隔（如test_ak_123,test_ak_456）");
        }

        // 3. 备注处理（无变动）
        String finalRemark = StringUtils.hasText(remark) ? remark : (operateType == 3 ? "批量禁用AK" : "批量启用AK");

        // 4. 循环处理AK（最小变动：日志operateType匹配枚举/数据库）
        int successCount = 0;
        for (String targetAk : akList) {
            SysUser sysUser = this.getByAccessKeyAllStatus(targetAk);
            if (sysUser == null) {
                throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "目标AK不存在：" + targetAk + "，批量操作已回滚！");
            }

            // 状态赋值（匹配SignConstants，无变动）
            Integer targetStatus = operateType == 3 ? SignConstants.AK_STATUS_DISABLE : SignConstants.AK_STATUS_ENABLE;
            sysUser.setStatus(targetStatus);
            this.updateById(sysUser);
            successCount++;

            // 日志记录：operateType直接用2/3（匹配枚举ENABLE_AK/DISABLE_AK，数据库operate_type=2/3）
            AkOperateLog log = new AkOperateLog();
            log.setTargetAk(targetAk);
            log.setOperateType(operateType); // 无变动：2=启用/3=禁用，和枚举/数据库完全匹配
            log.setOperator("test_ak_admin"); // 替换为实际管理员AK即可
            log.setRemark(finalRemark + "（批量操作-单条执行）");
            log.setOperateTime(LocalDateTime.now());
            akOperateLogService.save(log);
        }

        return successCount;
    }

    /**
     * 批量创建AK核心实现（事务保障：失败全回滚）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> batchCreateAk(Integer count, Integer permissionType) {
        // 1. 参数合法性校验
        if (count == null || count < 1 || count > 50) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "创建数量非法！仅支持1-50个AK");
        }
        // 权限默认值：1=只读，仅支持1/2两种权限
        Integer finalPermission = (permissionType == null || permissionType < 1 || permissionType > 2)
                ? 1 : permissionType;

        // 2. 批量生成AK/SK并入库
        Map<String, String> akResultMap = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            // 生成accessKey（UUID去横线后截取16位，保证唯一性）
            String accessKey = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            // 生成secretKey（32位高复杂度随机字符串）
            String secretKey = generateSecureSecretKey();

            // 3. 构建AK实体并保存
            SysUser sysUser = new SysUser();
            sysUser.setAccessKey(accessKey);
            sysUser.setSecretKey(secretKey);
            sysUser.setStatus(SignConstants.AK_STATUS_ENABLE); // 默认启用状态
            sysUser.setPermissionType(finalPermission);       // 初始权限
            sysUser.setCreateTime(LocalDateTime.now());

            sysUser.setUsername(accessKey); // 推荐：直接用accessKey作为username（唯一且无需额外生成）
            sysUser.setPassword("ak_default_pwd_123");

            this.save(sysUser);

            // 4. 记录操作日志（operateType=1，匹配枚举GENERATE_NEW_KEY=1）
            AkOperateLog log = new AkOperateLog();
            log.setTargetAk(accessKey);
            log.setOperateType(AkOperateTypeEnum.GENERATE_NEW_KEY.getCode());
            log.setOperator("test_ak_admin"); // 替换为实际管理员AK
            log.setRemark("批量创建AK（初始权限：" + (finalPermission == 1 ? "只读" : "读写") + "）");
            log.setOperateTime(LocalDateTime.now());
            akOperateLogService.save(log);

            // 5. 存入结果集（仅返回一次，前端需提示用户保存）
            akResultMap.put(accessKey, secretKey);
        }

        return akResultMap;
    }

    /**
     * 生成32位高复杂度secretKey
     */
    private String generateSecureSecretKey() {
        StringBuilder secretKey = new StringBuilder(SECRET_KEY_LENGTH);
        for (int i = 0; i < SECRET_KEY_LENGTH; i++) {
            // 随机选取字符集内的字符，保证随机性
            secretKey.append(KEY_CHARSET.charAt(SECURE_RANDOM.nextInt(KEY_CHARSET.length())));
        }
        return secretKey.toString();
    }

    /**
     * 分页查询AK核心实现
     */
    @Override
    public IPage<SysUser> batchQueryAk(Integer pageNum, Integer pageSize, String targetAkLike, Integer status, Integer permissionType) {
        // 1. 分页参数校验
        int finalPageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int finalPageSize = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;

        // 2. 构建分页对象
        Page<SysUser> page = new Page<>(finalPageNum, finalPageSize);

        // 3. 构建查询条件
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        // 过滤已删除的 AK：deleteStatus != 1（兼容旧数据为 null 的情况）
        queryWrapper.ne(SysUser::getDeleteStatus, 1);
        // 模糊匹配AK
        if (StringUtils.hasText(targetAkLike)) {
            queryWrapper.like(SysUser::getAccessKey, targetAkLike);
        }
        // 状态筛选（仅1=启用/3=禁用）
        if (status != null && (status == SignConstants.AK_STATUS_ENABLE || status == SignConstants.AK_STATUS_DISABLE)) {
            queryWrapper.eq(SysUser::getStatus, status);
        }
        // 权限筛选（仅1=只读/2=读写）
        if (permissionType != null && (permissionType == 1 || permissionType == 2)) {
            queryWrapper.eq(SysUser::getPermissionType, permissionType);
        }
        // 按创建时间倒序
        queryWrapper.orderByDesc(SysUser::getCreateTime);

        // 4. 执行分页查询
        return this.page(page, queryWrapper);
    }

    /**
     * AK有效性校验核心实现
     */
    @Override
    public Map<String, Object> checkAkValid(String accessKey) {
        Map<String, Object> resultMap = new HashMap<>();
        // 1. 校验AK是否为空
        if (!StringUtils.hasText(accessKey)) {
            resultMap.put("akExists", false);
            resultMap.put("akEnabled", false);
            resultMap.put("akNotExpired", false);
            resultMap.put("msg", "AK不能为空！");
            return resultMap;
        }

        // 2. 查询AK信息
        SysUser sysUser = this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getAccessKey, accessKey));

        // 3. 分维度校验
        // 3.1 AK是否存在
        boolean akExists = sysUser != null;
        resultMap.put("akExists", akExists);

        if (!akExists) {
            resultMap.put("akEnabled", false);
            resultMap.put("akNotExpired", false);
            resultMap.put("msg", "AK不存在！");
            return resultMap;
        }

        // 3.2 AK是否启用（status=1）
        boolean akEnabled = sysUser.getStatus() == SignConstants.AK_STATUS_ENABLE;
        resultMap.put("akEnabled", akEnabled);

        // 3.3 AK是否过期（expireTime为空=永久有效；不为空则需大于当前时间）
        boolean akNotExpired = true;
        LocalDateTime expireTime = sysUser.getExpireTime();
        if (expireTime != null) {
            akNotExpired = expireTime.isAfter(LocalDateTime.now());
        }
        resultMap.put("akNotExpired", akNotExpired);

        // 4. 组装最终提示
        if (akEnabled && akNotExpired) {
            resultMap.put("msg", "AK有效！权限类型：" + (sysUser.getPermissionType() == 1 ? "只读" : sysUser.getPermissionType() == 2 ? "读写" : "管理员"));
        } else if (!akEnabled) {
            resultMap.put("msg", "AK已禁用！");
        } else if (!akNotExpired) {
            resultMap.put("msg", "AK已过期！过期时间：" + expireTime);
        }

        return resultMap;
    }

    /**
     * AK逻辑删除核心实现（事务保障，失败回滚）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int logicDeleteAk(String targetAk, String remark) {
        // 1. 参数校验
        if (!StringUtils.hasText(targetAk)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "待删除的AK不能为空！");
        }
        String finalRemark = StringUtils.hasText(remark) ? remark : "逻辑删除AK（无备注）";

        // 2. 查询AK是否存在且未删除
        SysUser sysUser = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccessKey, targetAk)
                .eq(SysUser::getDeleteStatus, 0)); // 仅查询未删除的AK

        if (sysUser == null) {
            return 0; // AK不存在或已删除，返回0
        }

        // 3. 标记逻辑删除（delete_status=1）
        boolean updateSuccess = this.update(new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getDeleteStatus, 1)
                .set(SysUser::getUpdateTime, LocalDateTime.now())
                .eq(SysUser::getAccessKey, targetAk)
                .eq(SysUser::getDeleteStatus, 0));

        if (!updateSuccess) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "AK逻辑删除失败！");
        }

        // 4. 记录删除日志（operateType=4，建议新增枚举：DELETE_AK=4；暂用固定值4）
        AkOperateLog log = new AkOperateLog();
        log.setTargetAk(targetAk);
        log.setOperateType(4); // 4=删除AK（需同步更新数据库注释和枚举）
        log.setOperator("test_ak_admin"); // 替换为实际操作的管理员AK
        log.setRemark(finalRemark);
        log.setOperateTime(LocalDateTime.now());
        akOperateLogService.save(log);

        return 1; // 删除成功，返回1
    }

    @Override
    public void verifyAdminPermission(String accessKey, String sign, Long timestamp, String nonce) {
        // 1. 基础AK/SK校验
        this.verifyAkSk(accessKey, sign, timestamp, nonce);

        // 2. 管理员权限校验
        SysUser user = this.getByAccessKey(accessKey);
        if (user.getPermissionType() != 3) { // 3=管理员权限
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENY, "需要管理员权限");
        }
    }
}
