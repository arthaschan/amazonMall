<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="店铺" prop="shopId">
        <el-select
          v-model="queryParams.shopId"
          placeholder="全部店铺"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="shop in shopList"
            :key="shop.id"
            :label="shop.name"
            :value="shop.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="SKU" prop="sku">
        <el-input
          v-model="queryParams.sku"
          placeholder="请输入SKU"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="ASIN" prop="asin">
        <el-input
          v-model="queryParams.asin"
          placeholder="请输入ASIN"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="success" plain @click="handleSync" v-hasPermi="['amazon:inventory:sync']">
          <Icon icon="ep:refresh" class="mr-5px" /> 同步库存
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :row-class-name="tableRowClassName">
      <el-table-column label="SKU" align="center" prop="sku" width="150" />
      <el-table-column label="ASIN" align="center" prop="asin" width="130" />
      <el-table-column label="店铺" align="center" prop="shopName" width="120" />
      <el-table-column label="总库存" align="center" prop="quantity" width="100" />
      <el-table-column label="可用库存" align="center" prop="availableQuantity" width="100">
        <template #default="scope">
          <span :class="{ 'text-danger font-bold': scope.row.availableQuantity <= scope.row.reorderPoint }">
            {{ scope.row.availableQuantity }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="预留库存" align="center" prop="reservedQuantity" width="100" />
      <el-table-column label="可售天数" align="center" prop="daysOfSupply" width="120">
        <template #default="scope">
          <el-tag
            :type="getDaysOfSupplyType(scope.row.daysOfSupply)"
            effect="dark"
          >
            {{ scope.row.daysOfSupply != null ? scope.row.daysOfSupply + ' 天' : '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="库存状态" align="center" width="120">
        <template #default="scope">
          <el-tag :type="getStockStatusType(scope.row)">
            {{ getStockStatusLabel(scope.row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="更新时间"
        align="center"
        prop="updateTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 详情弹窗 -->
  <el-dialog v-model="detailVisible" title="库存详情" width="700px" v-loading="detailLoading">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="SKU">{{ detailData.sku }}</el-descriptions-item>
      <el-descriptions-item label="ASIN">{{ detailData.asin }}</el-descriptions-item>
      <el-descriptions-item label="店铺">{{ detailData.shopName }}</el-descriptions-item>
      <el-descriptions-item label="商品名称" :span="2">{{ detailData.title }}</el-descriptions-item>
      <el-descriptions-item label="总库存">{{ detailData.quantity }}</el-descriptions-item>
      <el-descriptions-item label="可用库存">
        <span :class="{ 'text-danger font-bold': detailData.availableQuantity <= detailData.reorderPoint }">
          {{ detailData.availableQuantity }}
        </span>
      </el-descriptions-item>
      <el-descriptions-item label="预留库存">{{ detailData.reservedQuantity }}</el-descriptions-item>
      <el-descriptions-item label="在途库存">{{ detailData.inboundQuantity || 0 }}</el-descriptions-item>
      <el-descriptions-item label="可售天数">
        <el-tag :type="getDaysOfSupplyType(detailData.daysOfSupply)">
          {{ detailData.daysOfSupply != null ? detailData.daysOfSupply + ' 天' : '-' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="日均销量">{{ detailData.avgDailySales || '-' }}</el-descriptions-item>
      <el-descriptions-item label="补货点">{{ detailData.reorderPoint || '-' }}</el-descriptions-item>
      <el-descriptions-item label="FNSKU">{{ detailData.fnsku || '-' }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ dateFormatter(detailData.updateTime) }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { InventoryApi } from '@/api/amazon/inventory'
import { ShopApi } from '@/api/amazon/shop'

const message = useMessage()

/** 店铺列表 */
const shopList = ref([])

/** 可售天数标签类型 */
const getDaysOfSupplyType = (days: number | null) => {
  if (days == null) return 'info'
  if (days <= 7) return 'danger'
  if (days <= 14) return 'warning'
  if (days <= 30) return ''
  return 'success'
}

/** 库存状态判断 */
const getStockStatusLabel = (row) => {
  if (row.availableQuantity <= 0) return '缺货'
  if (row.reorderPoint && row.availableQuantity <= row.reorderPoint) return '低库存'
  return '正常'
}

const getStockStatusType = (row) => {
  if (row.availableQuantity <= 0) return 'danger'
  if (row.reorderPoint && row.availableQuantity <= row.reorderPoint) return 'warning'
  return 'success'
}

/** 低库存行高亮 */
const tableRowClassName = ({ row }) => {
  if (row.availableQuantity <= 0) return 'danger-row'
  if (row.reorderPoint && row.availableQuantity <= row.reorderPoint) return 'warning-row'
  return ''
}

/** 查询列表 */
const loading = ref(true)
const total = ref(0)
const list = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  shopId: undefined,
  sku: undefined,
  asin: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await InventoryApi.getInventoryPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 加载店铺列表 */
const loadShopList = async () => {
  try {
    const data = await ShopApi.getShopPage({ pageNo: 1, pageSize: 100 })
    shopList.value = data.list
  } catch (error) {
    console.error('加载店铺列表失败', error)
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 同步库存 */
const handleSync = async () => {
  if (!queryParams.shopId) {
    message.warning('请先选择店铺')
    return
  }
  try {
    await InventoryApi.syncInventory(queryParams.shopId)
    message.success('库存同步任务已启动')
    setTimeout(() => getList(), 2000)
  } catch (error) {
    message.error('同步失败')
  }
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref({})

const openDetail = async (row) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await InventoryApi.getInventory(row.id)
  } finally {
    detailLoading.value = false
  }
}

/** 初始化 */
onMounted(() => {
  loadShopList()
  getList()
})
</script>

<style scoped>
:deep(.warning-row) {
  background-color: #fdf6ec !important;
}
:deep(.danger-row) {
  background-color: #fef0f0 !important;
}
</style>
