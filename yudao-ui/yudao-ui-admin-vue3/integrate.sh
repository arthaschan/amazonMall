#!/usr/bin/env bash
# ============================================================================
# AmazonOps AI - 一键集成脚本
#
# 将 Amazon 运营模块（API、页面、路由）复制到完整的 yudao-ui-admin-vue3 项目中。
#
# 用法:
#   ./integrate.sh <目标项目路径>
#
# 示例:
#   ./integrate.sh /Users/arthas/git/yudao-ui-admin-vue3
#   ./integrate.sh ../yudao-ui-admin-vue3
# ============================================================================

set -euo pipefail

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 当前脚本所在目录（即 Amazon 模块的开发目录）
SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"

echo ""
echo -e "${CYAN}======================================================${NC}"
echo -e "${CYAN}   AmazonOps AI - 一键集成到 yudao-ui-admin-vue3${NC}"
echo -e "${CYAN}======================================================${NC}"
echo ""

# -------------------------------------------------------------------------
# 1. 解析并验证目标路径
# -------------------------------------------------------------------------
if [ $# -lt 1 ]; then
  echo -e "${RED}错误: 请提供目标 yudao-ui-admin-vue3 项目路径${NC}"
  echo ""
  echo "用法: ./integrate.sh <目标项目路径>"
  echo "示例: ./integrate.sh /Users/arthas/git/yudao-ui-admin-vue3"
  exit 1
fi

TARGET_DIR="$1"

# 将相对路径转为绝对路径
if [[ ! "$TARGET_DIR" = /* ]]; then
  TARGET_DIR="$(cd "$(dirname "$TARGET_DIR")" && pwd)/$(basename "$TARGET_DIR")"
fi

echo -e "${YELLOW}源目录:   ${SOURCE_DIR}${NC}"
echo -e "${YELLOW}目标目录: ${TARGET_DIR}${NC}"
echo ""

# 检查目标目录是否存在
if [ ! -d "$TARGET_DIR" ]; then
  echo -e "${RED}错误: 目标目录不存在 -> ${TARGET_DIR}${NC}"
  exit 1
fi

# 检查目标是否为 yudao-ui-admin-vue3 项目（检查 package.json 或 src 目录）
if [ ! -f "$TARGET_DIR/package.json" ] && [ ! -d "$TARGET_DIR/src" ]; then
  echo -e "${RED}错误: 目标目录不像是 yudao-ui-admin-vue3 项目（未找到 package.json 或 src 目录）${NC}"
  exit 1
fi

# 检查目标 src 目录
if [ ! -d "$TARGET_DIR/src" ]; then
  echo -e "${RED}错误: 目标目录下缺少 src 目录 -> ${TARGET_DIR}/src${NC}"
  exit 1
fi

# -------------------------------------------------------------------------
# 2. 检查源文件完整性
# -------------------------------------------------------------------------
echo -e "${CYAN}[1/5] 检查源文件完整性...${NC}"

MISSING=0

if [ ! -d "$SOURCE_DIR/src/api/amazon" ]; then
  echo -e "  ${RED}✗ 缺少 src/api/amazon/${NC}"
  MISSING=1
fi

if [ ! -d "$SOURCE_DIR/src/views/amazon" ]; then
  echo -e "  ${RED}✗ 缺少 src/views/amazon/${NC}"
  MISSING=1
fi

if [ ! -f "$SOURCE_DIR/src/router/modules/amazon.ts" ]; then
  echo -e "  ${RED}✗ 缺少 src/router/modules/amazon.ts${NC}"
  MISSING=1
fi

if [ "$MISSING" -eq 1 ]; then
  echo -e "${RED}错误: 源文件不完整，请检查当前目录${NC}"
  exit 1
fi

# 统计文件数
API_COUNT=$(find "$SOURCE_DIR/src/api/amazon" -type f | wc -l | tr -d ' ')
VIEW_COUNT=$(find "$SOURCE_DIR/src/views/amazon" -type f | wc -l | tr -d ' ')

echo -e "  ${GREEN}✓ API 文件:   ${API_COUNT} 个${NC}"
echo -e "  ${GREEN}✓ 页面文件:   ${VIEW_COUNT} 个${NC}"
echo -e "  ${GREEN}✓ 路由配置:   amazon.ts${NC}"
echo ""

# -------------------------------------------------------------------------
# 3. 备份已存在的目标文件（如果有的话）
# -------------------------------------------------------------------------
echo -e "${CYAN}[2/5] 检查目标目录...${NC}"

BACKUP_DIR=""
if [ -d "$TARGET_DIR/src/api/amazon" ] || [ -d "$TARGET_DIR/src/views/amazon" ] || \
   [ -f "$TARGET_DIR/src/router/modules/amazon.ts" ]; then
  BACKUP_DIR="$TARGET_DIR/.amazon-backup-$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$BACKUP_DIR"
  echo -e "  ${YELLOW}! 检测到目标已存在 Amazon 模块，将备份到: ${BACKUP_DIR}${NC}"

  [ -d "$TARGET_DIR/src/api/amazon" ] && \
    cp -r "$TARGET_DIR/src/api/amazon" "$BACKUP_DIR/api-amazon" && \
    echo -e "  ${GREEN}✓ 已备份 api/amazon${NC}"

  [ -d "$TARGET_DIR/src/views/amazon" ] && \
    cp -r "$TARGET_DIR/src/views/amazon" "$BACKUP_DIR/views-amazon" && \
    echo -e "  ${GREEN}✓ 已备份 views/amazon${NC}"

  [ -f "$TARGET_DIR/src/router/modules/amazon.ts" ] && \
    cp "$TARGET_DIR/src/router/modules/amazon.ts" "$BACKUP_DIR/amazon.ts" && \
    echo -e "  ${GREEN}✓ 已备份 amazon.ts${NC}"
else
  echo -e "  ${GREEN}✓ 目标目录无已有 Amazon 模块，无需备份${NC}"
fi
echo ""

# -------------------------------------------------------------------------
# 4. 复制文件到目标项目
# -------------------------------------------------------------------------
echo -e "${CYAN}[3/5] 复制文件到目标项目...${NC}"

# 复制 API 文件
mkdir -p "$TARGET_DIR/src/api/amazon"
cp -r "$SOURCE_DIR/src/api/amazon/"* "$TARGET_DIR/src/api/amazon/"
echo -e "  ${GREEN}✓ 已复制 src/api/amazon/ (${API_COUNT} 个文件)${NC}"

# 复制页面文件
mkdir -p "$TARGET_DIR/src/views/amazon"
cp -r "$SOURCE_DIR/src/views/amazon/"* "$TARGET_DIR/src/views/amazon/"
echo -e "  ${GREEN}✓ 已复制 src/views/amazon/ (${VIEW_COUNT} 个文件)${NC}"

# 复制路由配置
mkdir -p "$TARGET_DIR/src/router/modules"
cp "$SOURCE_DIR/src/router/modules/amazon.ts" "$TARGET_DIR/src/router/modules/amazon.ts"
echo -e "  ${GREEN}✓ 已复制 src/router/modules/amazon.ts${NC}"
echo ""

# -------------------------------------------------------------------------
# 5. 安装额外依赖
# -------------------------------------------------------------------------
echo -e "${CYAN}[4/5] 安装额外依赖 (echarts)...${NC}"

if [ -f "$TARGET_DIR/package.json" ]; then
  # 检查 echarts 是否已安装
  if grep -q '"echarts"' "$TARGET_DIR/package.json" 2>/dev/null; then
    echo -e "  ${GREEN}✓ echarts 已在 package.json 中，跳过安装${NC}"
  else
    cd "$TARGET_DIR"
    if npm install echarts --save 2>&1 | tail -3; then
      echo -e "  ${GREEN}✓ echarts 安装成功${NC}"
    else
      echo -e "  ${YELLOW}! echarts 安装失败，请手动执行: cd ${TARGET_DIR} && npm install echarts${NC}"
    fi
    cd "$SOURCE_DIR"
  fi
else
  echo -e "  ${YELLOW}! 未找到 package.json，请手动安装 echarts${NC}"
fi
echo ""

# -------------------------------------------------------------------------
# 6. 完成提示
# -------------------------------------------------------------------------
echo -e "${CYAN}[5/5] 集成完成!${NC}"
echo ""
echo -e "${GREEN}======================================================${NC}"
echo -e "${GREEN}   AmazonOps AI 已成功集成到目标项目!${NC}"
echo -e "${GREEN}======================================================${NC}"
echo ""
echo -e "已复制的文件:"
echo -e "  - src/api/amazon/       (8 个 API 服务模块)"
echo -e "  - src/views/amazon/     (26 个 Vue 页面组件)"
echo -e "  - src/router/modules/amazon.ts  (21 条路由)"
echo ""
if [ -n "$BACKUP_DIR" ]; then
  echo -e "${YELLOW}旧文件已备份到: ${BACKUP_DIR}${NC}"
  echo ""
fi
echo -e "后续步骤:"
echo -e "  1. 在目标项目中配置 Vite 代理（参考 VITE_PROXY_CONFIG.md）:"
echo -e "     ${CYAN}'/admin-api' => 'http://localhost:48080'${NC}"
echo ""
echo -e "  2. 在后台管理系统的「菜单管理」中添加 Amazon 菜单及权限标识"
echo ""
echo -e "  3. 启动项目:"
echo -e "     ${CYAN}cd ${TARGET_DIR}${NC}"
echo -e "     ${CYAN}npm install${NC}"
echo -e "     ${CYAN}npm run dev${NC}"
echo ""
echo -e "  4. 访问 ${CYAN}http://localhost:80/#/amazon/dashboard${NC} 查看运营看板"
echo ""
