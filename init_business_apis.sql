-- 初始化业务接口数据
-- 执行此SQL脚本，在数据库中注册3个测试业务接口

-- 1. 天气查询接口
INSERT INTO api_info (api_name, api_path, method, api_desc, status, create_time, update_time)
VALUES (
    '天气查询接口',
    '/api/business/weather/query',
    'GET',
    '查询指定城市的天气信息，返回温度、天气、湿度、风速等信息',
    1,
    NOW(),
    NOW()
);

-- 2. 时间查询接口
INSERT INTO api_info (api_name, api_path, method, api_desc, status, create_time, update_time)
VALUES (
    '时间查询接口',
    '/api/business/time/current',
    'GET',
    '获取当前时间信息，支持指定时区，返回当前时间、时间戳、日期、时间等',
    1,
    NOW(),
    NOW()
);

-- 3. 随机数生成接口
INSERT INTO api_info (api_name, api_path, method, api_desc, status, create_time, update_time)
VALUES (
    '随机数生成接口',
    '/api/business/random/generate',
    'GET',
    '生成指定范围内的随机数，支持指定最小值、最大值和生成数量',
    1,
    NOW(),
    NOW()
);

-- 查询验证
SELECT id, api_name, api_path, method, api_desc, status 
FROM api_info 
WHERE api_path LIKE '/api/business%'
ORDER BY id DESC;

