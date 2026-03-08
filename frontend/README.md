# 智安临评 - 前端

## 技术栈

- Vue 3
- Element Plus
- Vue Router
- Pinia
- Axios
- Vite

## 安装依赖

```bash
npm install
```

## 开发运行

```bash
npm run dev
```

前端服务将在 http://localhost:3000 启动

## 构建生产版本

```bash
npm run build
```

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API接口封装
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── layouts/          # 布局组件
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia状态管理
│   ├── utils/            # 工具函数
│   ├── views/            # 页面组件
│   │   ├── assessment/   # 评估相关页面
│   │   ├── patient/      # 患者管理页面
│   │   ├── template/     # 模板管理页面
│   │   └── user/         # 用户管理页面
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html
├── package.json
└── vite.config.js
```

## 功能模块

1. **用户登录与权限管理**
   - 用户登录
   - 基于角色的功能访问控制

2. **患者信息管理**
   - 患者列表查询
   - 患者信息新增/编辑
   - 患者信息删除（管理员）

3. **评估模板管理**
   - 模板列表查询
   - 模板详情查看
   - 模板字段配置查看

4. **评估数据采集**
   - 创建评估记录
   - 动态表单填写
   - 保存草稿/提交评估

5. **评估历史记录**
   - 患者评估历史查询
   - 评估记录详情查看
   - 报告导出（PDF/Word）

6. **用户管理**（管理员）
   - 用户列表查询
   - 用户信息管理

## 注意事项

- 确保后端服务运行在 http://localhost:8080
- 后端API路径前缀为 `/api`
- 前端通过代理转发API请求，配置在 `vite.config.js` 中

