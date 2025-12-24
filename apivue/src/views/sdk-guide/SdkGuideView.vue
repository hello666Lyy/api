<script setup lang="ts">
import { ref } from 'vue'
import { Document, Download, Connection } from '@element-plus/icons-vue'
import { downloadSdk } from '@/api/sdk'
import { ElMessage } from 'element-plus'

const activeStep = ref(0)
const activeTab = ref('maven')

// API接口列表
const apiList = ref([
  {
    name: '天气查询接口',
    path: '/api/business/weather/query',
    method: 'GET',
    description: '查询指定城市的天气信息'
  },
  {
    name: '时间查询接口',
    path: '/api/business/time/current',
    method: 'GET',
    description: '获取当前时间信息'
  },
  {
    name: '随机数生成接口',
    path: '/api/business/random/generate',
    method: 'GET',
    description: '生成指定范围内的随机数'
  }
])

const mavenDependency = `<dependency>
  <groupId>com.org</groupId>
  <artifactId>api_sdk</artifactId>
  <version>1.0-SNAPSHOT</version>
  <scope>system</scope>
  <systemPath>\${project.basedir}/lib/api_sdk-1.0-SNAPSHOT.jar</systemPath>
</dependency>`

const springConfigExample = `# application.yml
server:
  port: 8082

api:
  baseUrl: http://localhost:8081   # 后端 API 服务地址
  accessKey: UYdonKph9RMptCKJ      # 你的 AK
  secretKey: 4pSuhK9GqyWwb2USBQn0pO25uVDiYFjL  # 你的 SK

// ApiSdkConfig.java
@Configuration
public class ApiSdkConfig {

    @Value("\${api.baseUrl}")
    private String baseUrl;

    @Value("\${api.accessKey}")
    private String accessKey;

    @Value("\${api.secretKey}")
    private String secretKey;

    @Bean
    public ApiClient apiClient() {
        ApiConfig config = ApiConfig.builder()
                .baseUrl(baseUrl)
                .accessKey(accessKey)
                .secretKey(secretKey)
                .connectTimeout(5000)
                .readTimeout(10000)
                .enableLog(true)   // 建议开发环境打开日志，方便排查
                .build();
        return new ApiClient(config);
    }
}`

const exampleCode = `@Service
public class DemoService {

    @Autowired
    private ApiClient apiClient;

    // 获取用户信息
    public UserInfoVO getUserInfo() {
        UserService userService = apiClient.getUserService();
        return userService.getUserInfo();
    }

    // 查询天气
    public Map<String, Object> queryWeather(String city) {
        BusinessApiService businessService = apiClient.getBusinessApiService();
        return businessService.queryWeather(city);
    }

    // 获取当前时间
    public Map<String, Object> getCurrentTime(String timezone) {
        BusinessApiService businessService = apiClient.getBusinessApiService();
        return businessService.getCurrentTime(timezone);
    }

    // 生成随机数（需要开通对应接口权限）
    public Map<String, Object> generateRandom(int min, int max, int count) {
        BusinessApiService businessService = apiClient.getBusinessApiService();
        return businessService.generateRandom(min, max, count);
    }
}`

const gradleDependency = `dependencies {
    implementation 'com.org:api_sdk:1.0-SNAPSHOT'
}`

const pomXmlExample = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>com.org</groupId>
            <artifactId>api_sdk</artifactId>
            <version>1.0-SNAPSHOT</version>
            <scope>system</scope>
            <systemPath>\${project.basedir}/lib/api_sdk-1.0-SNAPSHOT.jar</systemPath>
        </dependency>
    </dependencies>
</project>`

// 下载SDK
async function handleDownloadSdk() {
  try {
    const blob = await downloadSdk()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'api_sdk-1.0-SNAPSHOT.jar'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('SDK下载成功')
  } catch (error: any) {
    console.error('SDK下载失败:', error)
    const errorMsg = error.message || 'SDK下载失败，请稍后重试'
    ElMessage.error(errorMsg)
  }
}

// 复制代码
function copyCode(code: string) {
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success('代码已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}
</script>

<template>
  <div class="sdk-guide-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>SDK使用指南</h2>
        <p>快速集成API平台SDK，轻松调用业务接口</p>
      </div>
      <el-icon :size="48" color="#409eff"><Document /></el-icon>
    </div>

    <!-- 快速开始步骤 -->
    <el-card shadow="never" class="steps-card">
      <template #header>
        <div class="card-header">
          <el-icon><Connection /></el-icon>
          <span>快速开始</span>
        </div>
      </template>
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step title="下载SDK" description="在下方点击按钮下载 JAR 包" />
        <el-step title="放入项目" description="将 JAR 放到项目 lib 目录" />
        <el-step title="引入依赖" description="复制 Maven 依赖到 pom.xml" />
        <el-step title="配置与调用" description="按示例配置 yml 和 DemoService" />
      </el-steps>
    </el-card>

    <!-- 下载SDK卡片 -->
    <el-card shadow="never" class="download-card">
      <template #header>
        <div class="card-header">
          <span>下载SDK</span>
        </div>
      </template>
      <div class="download-content">
        <p>您可以通过以下方式获取SDK：</p>
        <el-space>
          <el-button type="primary" :icon="Download" size="large" @click="handleDownloadSdk">
            直接下载JAR包
          </el-button>
          <el-tag type="info">版本: 1.0-SNAPSHOT</el-tag>
        </el-space>
        <el-alert type="info" :closable="false" show-icon style="margin-top: 16px">
          <template #default>
            <p style="margin: 0;">下载后，将jar包添加到项目的classpath中即可使用。</p>
          </template>
        </el-alert>
      </div>
    </el-card>

    <!-- 代码示例卡片 -->
    <el-card shadow="never" class="code-card">
      <template #header>
        <div class="card-header">
          <span>代码示例</span>
        </div>
      </template>
      
      <el-tabs v-model="activeTab">
        <el-tab-pane label="Maven依赖" name="maven">
          <div class="code-block">
            <div class="code-header">
              <span>Maven依赖配置</span>
              <el-button type="primary" link size="small" @click="copyCode(mavenDependency)">
                复制代码
              </el-button>
            </div>
            <pre><code>{{ mavenDependency }}</code></pre>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="Gradle依赖" name="gradle">
          <div class="code-block">
            <div class="code-header">
              <span>Gradle依赖配置</span>
              <el-button type="primary" link size="small" @click="copyCode(gradleDependency)">
                复制代码
              </el-button>
            </div>
            <pre><code>{{ gradleDependency }}</code></pre>
          </div>
        </el-tab-pane>

        <el-tab-pane label="Spring Boot配置" name="spring">
          <div class="code-block">
            <div class="code-header">
              <span>application.yml + ApiSdkConfig</span>
              <el-button type="primary" link size="small" @click="copyCode(springConfigExample)">
                复制代码
              </el-button>
            </div>
            <pre><code>{{ springConfigExample }}</code></pre>
          </div>
        </el-tab-pane>

        <el-tab-pane label="业务代码示例" name="example">
          <div class="code-block">
            <div class="code-header">
              <span>DemoService 示例</span>
              <el-button type="primary" link size="small" @click="copyCode(exampleCode)">
                复制代码
              </el-button>
            </div>
            <pre><code>{{ exampleCode }}</code></pre>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="pom.xml示例" name="pom">
          <div class="code-block">
            <div class="code-header">
              <span>完整的pom.xml示例</span>
              <el-button type="primary" link size="small" @click="copyCode(pomXmlExample)">
                复制代码
              </el-button>
            </div>
            <pre><code>{{ pomXmlExample }}</code></pre>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 使用说明 -->
    <el-card shadow="never" class="help-card">
      <template #header>
        <div class="card-header">
          <span>使用说明</span>
        </div>
      </template>
      <div class="help-content">
        <el-alert type="info" :closable="false" show-icon>
          <template #default>
            <div>
              <p style="margin: 0 0 12px 0; font-weight: 600;">常见问题：</p>
              <ul style="margin: 0; padding-left: 20px; line-height: 1.8;">
                <li><strong>404：AK不存在或用户已禁用</strong>：检查 <code>application.yml</code> 中 AK 是否正确，后台是否启用了该 AK。</li>
                <li><strong>403：接口权限不足</strong>：在后台为当前 AK 勾选对应接口（如时间查询、随机数生成等）。</li>
              </ul>
            </div>
          </template>
        </el-alert>
      </div>
    </el-card>

    <!-- API接口列表 -->
    <el-card shadow="never" class="api-list-card">
      <template #header>
        <div class="card-header">
          <span>可用业务接口</span>
        </div>
      </template>
      <el-table :data="apiList" border>
        <el-table-column prop="name" label="接口名称" width="200" />
        <el-table-column prop="path" label="接口路径" />
        <el-table-column prop="method" label="请求方式" width="120">
          <template #default="{ row }">
            <el-tag :type="row.method === 'GET' ? 'success' : 'primary'">
              {{ row.method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="接口描述" />
      </el-table>
    </el-card>
  </div>
</template>


<style scoped>
.sdk-guide-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
}

.page-header p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.steps-card,
.download-card,
.code-card,
.help-card,
.api-list-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.download-content {
  padding: 8px 0;
}

.code-block {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
  font-weight: 600;
}

.code-block pre {
  margin: 0;
  padding: 16px;
  background: #fafafa;
  overflow-x: auto;
}

.code-block code {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
}

.help-content {
  padding: 8px 0;
}
</style>

