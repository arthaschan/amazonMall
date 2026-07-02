# Vite 代理配置

AmazonOps AI 前端页面通过 `/admin-api` 前缀向后端发起请求，需在 `vite.config.ts` 中配置开发代理。

## 配置方式

在 `vite.config.ts` 的 `server.proxy` 中添加以下条目：

```typescript
// vite.config.ts
export default defineConfig({
  // ...
  server: {
    port: 80,
    proxy: {
      // AmazonOps AI 后端代理（及其他 yudao 业务接口）
      '/admin-api': {
        target: 'http://localhost:48080',
        changeOrigin: true
        // 若后端不使用 /admin-api 前缀，可添加 rewrite：
        // rewrite: (path) => path.replace(/^\/admin-api/, '')
      }
    }
  }
})
```

## 说明

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 代理路径 | `/admin-api` | 前端所有 API 调用的统一前缀 |
| 后端地址 | `http://localhost:48080` | Spring Boot 后端服务地址 |
| changeOrigin | `true` | 修改请求头中的 Host 字段，避免跨域 |

## 后端环境要求

启动前端前，请确保后端服务已运行：

```bash
# 后端（yudao-server）默认端口 48080
cd yudao-server
mvn spring-boot:run
# 或
java -jar target/yudao-server.jar
```

## API 路径映射

前端请求路径与后端 Controller 的对应关系：

| 前端请求 | 后端 Controller |
|----------|-----------------|
| `GET  /admin-api/amazon/shop/page` | `AmazonShopController.getShopPage()` |
| `GET  /admin-api/amazon/shop/get` | `AmazonShopController.getShop()` |
| `POST /admin-api/amazon/shop/create` | `AmazonShopController.createShop()` |
| `PUT  /admin-api/amazon/shop/update` | `AmazonShopController.updateShop()` |
| `DELETE /admin-api/amazon/shop/delete` | `AmazonShopController.deleteShop()` |
| `POST /admin-api/amazon/shop/test-connection` | `AmazonShopController.testConnection()` |
| `GET  /admin-api/amazon/listing/page` | `AmazonListingController.getListingPage()` |
| `GET  /admin-api/amazon/order/page` | `AmazonOrderController.getOrderPage()` |
| `GET  /admin-api/amazon/inventory/page` | `AmazonInventoryController.getInventoryPage()` |
| `GET  /admin-api/amazon/research/niche` | `AmazonResearchController.getNichePage()` |
| `GET  /admin-api/amazon/ad/campaign-page` | `AmazonAdController.getCampaignPage()` |
| `GET  /admin-api/amazon/review/page` | `AmazonReviewController.getReviewPage()` |
| `GET  /admin-api/amazon/report/dashboard` | `AmazonReportController.getDashboard()` |
| `POST /admin-api/amazon/report/ai-chat` | `AmazonReportController.aiChat()` |

## 多环境配置

如需区分开发/测试/生产环境，可通过 `.env.development` 和 `.env.production` 管理后端地址：

```bash
# .env.development
VITE_API_BASE_URL = http://localhost:48080

# .env.production
VITE_API_BASE_URL = https://api.example.com
```

然后在 `vite.config.ts` 中引用：

```typescript
proxy: {
  '/admin-api': {
    target: process.env.VITE_API_BASE_URL || 'http://localhost:48080',
    changeOrigin: true
  }
}
```

## 常见问题

**Q: 请求 404**
检查后端是否已启动，以及 Controller 的 `@RequestMapping` 是否包含 `/admin-api` 前缀。

**Q: CORS 报错**
确认 `changeOrigin: true` 已设置；若后端有自己的 CORS 配置，两端需保持一致。

**Q: WebSocket 连接失败（AI 助手页面）**
如果 AI Chat 使用 WebSocket，需额外添加 ws 代理：

```typescript
'/admin-api': {
  target: 'http://localhost:48080',
  changeOrigin: true,
  ws: true
}
```
