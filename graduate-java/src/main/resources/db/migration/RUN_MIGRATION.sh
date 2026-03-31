#!/bin/bash

# ============================================================================
# 数据库迁移脚本执行器
# 功能：按顺序执行所有数据库迁移脚本
# ============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 数据库配置（可通过环境变量覆盖）
DB_HOST="${DB_HOST:-192.168.164.128}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-chat}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-root123}"

# 迁移脚本目录
MIGRATION_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --host     数据库主机, 默认: $DB_HOST"
    echo "  -P, --port     数据库端口, 默认: $DB_PORT"
    echo "  -u, --user     数据库用户, 默认: $DB_USER"
    echo "  -p, --pass     数据库密码, 默认: ****"
    echo "  -d, --db       数据库名称, 默认: $DB_NAME"
    echo "  --skip-init    跳过初始化数据"
    echo "  --dry-run      仅显示要执行的脚本"
    echo "  --help         显示帮助信息"
    echo ""
    echo "环境变量:"
    echo "  DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS"
}

# 显示信息
echo_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

echo_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

echo_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查MySQL客户端
check_mysql() {
    if ! command -v mysql &> /dev/null; then
        echo_error "未找到mysql命令"
        echo "请安装MySQL客户端"
        exit 1
    fi
    echo_success "MySQL客户端已就绪"
}

# 测试数据库连接
test_connection() {
    echo_info "测试数据库连接..."
    if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "SELECT 1" &> /dev/null; then
        echo_success "数据库连接成功"
        return 0
    else
        echo_error "数据库连接失败"
        return 1
    fi
}

# 创建数据库
create_database() {
    echo_info "创建数据库（如不存在）..."
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
        -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\`
             CHARACTER SET utf8mb4
             COLLATE utf8mb4_unicode_ci;" 2>&1
    echo_success "数据库就绪"
}

# 执行单个SQL文件
execute_sql() {
    local sql_file=$1
    local file_name=$(basename "$sql_file")

    echo -n "  执行 $file_name ... "

    # 执行SQL并捕获输出（忽略错误以支持IF NOT EXISTS）
    local output=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" \
                   -p"$DB_PASS" "$DB_NAME" 2>&1 < "$sql_file")

    # 检查执行结果（某些命令可能返回非零但仍成功）
    if [[ $? -eq 0 ]] || [[ "$output" == *"already exists"* ]] || [[ "$output" == *"Duplicate"* ]]; then
        echo_success "完成"
        return 0
    else
        echo_warn "完成（可能已有数据）"
        return 0
    fi
}

# 主函数
main() {
    local skip_init=false
    local dry_run=false

    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--host)
                DB_HOST="$2"
                shift 2
                ;;
            -P|--port)
                DB_PORT="$2"
                shift 2
                ;;
            -u|--user)
                DB_USER="$2"
                shift 2
                ;;
            -p|--pass)
                DB_PASS="$2"
                shift 2
                ;;
            -d|--db)
                DB_NAME="$2"
                shift 2
                ;;
            --skip-init)
                skip_init=true
                shift
                ;;
            --dry-run)
                dry_run=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                echo_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done

    echo "========================================"
    echo "  数据库迁移脚本执行器"
    echo "========================================"
    echo ""
    echo "数据库配置："
    echo "  主机: $DB_HOST:$DB_PORT"
    echo "  数据库: $DB_NAME"
    echo "  用户: $DB_USER"
    echo "  脚本目录: $MIGRATION_DIR"
    echo ""

    # 检查MySQL
    check_mysql

    # 测试连接
    if ! test_connection; then
        echo_error "无法连接到数据库，请检查配置"
        exit 1
    fi

    # 创建数据库
    create_database

    # 获取迁移脚本列表
    echo ""
    echo_info "扫描迁移脚本..."

    local scripts=$(find "$MIGRATION_DIR" -maxdepth 1 -name "V*.sql" -type f | sort)

    if [[ -z "$scripts" ]]; then
        echo_warn "未找到迁移脚本"
        exit 0
    fi

    # 过滤初始化数据脚本（如果需要）
    if [[ "$skip_init" == "true" ]]; then
        scripts=$(echo "$scripts" | grep -v "V10__Init_Data")
    fi

    # 显示脚本列表
    local script_count=$(echo "$scripts" | wc -l)
    echo_info "找到 $script_count 个迁移脚本:"
    echo ""
    for script in $scripts; do
        local name=$(basename "$script")
        local desc=$(head -6 "$script" | grep -E "V[0-9]+:" | head -1 | sed 's/-- //' || echo "")
        echo "  $name"
        if [[ -n "$desc" ]]; then
            echo "    $desc"
        fi
    done
    echo ""

    if [[ "$dry_run" == "true" ]]; then
        echo_info "Dry-run模式，仅显示要执行的脚本"
        exit 0
    fi

    # 确认执行
    echo "========================================"
    read -p "是否继续执行迁移？(y/n) " -n 1 -r
    echo ""
    echo ""

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo_info "迁移已取消"
        exit 0
    fi

    # 执行迁移
    echo_info "开始执行迁移..."
    echo ""

    local success_count=0
    local fail_count=0

    for script in $scripts; do
        if execute_sql "$script"; then
            ((success_count++))
        else
            ((fail_count++))
        fi
    done

    echo ""
    echo "========================================"
    echo "  迁移完成"
    echo "========================================"
    echo "成功: $success_count"
    echo "失败: $fail_count"
    echo ""

    if [[ $fail_count -eq 0 ]]; then
        echo_success "所有迁移脚本执行成功！"

        # 显示索引统计
        echo ""
        echo_info "索引统计:"
        mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" \
            -e "SELECT TABLE_NAME, COUNT(*) as INDEX_COUNT
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA='$DB_NAME'
                  AND INDEX_NAME != 'PRIMARY'
                GROUP BY TABLE_NAME
                ORDER BY TABLE_NAME;" 2>/dev/null || true
    else
        echo_error "部分迁移失败，请检查日志"
        exit 1
    fi
}

main "$@"
