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
      <el-form-item label="订单号" prop="amazonOrderId">
        <el-input
          v-model="queryParams.amazonOrderId"
          placeholder="请输入亚马逊订单号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="订单状态" prop="orderStatus">
        <el-select
          v-model="queryParams.orderStatus"
          placeholder="全部状态"
          clearable
          class="!w-240px"
        >
          <el-option label="待处理" value="Pending" />
          <el-option label="未发货" value="Unshipped" />
          <el-option label="已发货" value="Shipped" />
          <el-option label="已取消" value="Canceled" />
        </el-select>
      </el-form-item>
      <el-form-item label="下单时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="success" plain @click="handleSync">
          <Icon icon="ep:refresh" class="mr-5px" /> 同步订单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="订单号" align="center" prop="amazonOrderId" width="180" />
      <el-table-column label="商品信息" align="left" min-width="250">
        <template #default="scope">
          <div v-for="item in scope.row.items" :key="item.sku" class="mb-5px">
            <span class="font-bold">{{ item.sku }}</span> x {{ item.quantity }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="店铺" align="center" prop="shopName" width="120" />
      <el-table-column label="订单金额" align="center" prop="orderTotal" width="120">
        <template #default="scope">
          ${{ scope.row.orderTotal }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="orderStatus" width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.orderStatus)">
            {{ getStatusLabel(scope.row.orderStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="下单时间"
        align="center"
        prop="purchaseDate"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="配送方式" align="center" prop="fulfillmentChannel" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.fulfillmentChannel === 'AFN' ? 'success' : 'info'">
            {{ scope.row.fulfillmentChannel === 'AFN' ? 'FBA' : 'FBM' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">
            详情
          </el-button>
          <el-button
            link
            type="primary"
            @click="handleShip(scope.row)"
            v-if="scope.row.orderStatus === 'Unshipped' && scope.row.fulfillmentChannel === 'MFN'"
          >
            发货
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
  <OrderDetail ref="detailRef" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { OrderApi } from '@/api/amazon/order'
import { ShopApi } from '@/api/amazon/shop'
import OrderDetail from './OrderDetail.vue'

const message = useMessage()

const statusMap = {
  'Pending': { label: '待处理', type: 'info' },
  'Unshipped': { label: '未发货', type: 'warning' },
  'Shipped': { label: '已发货', type: 'success' },
  'Canceled': { label: '已取消', type: 'info' }
}

const getStatusLabel = (status: string) => {
  return statusMap[status]?.label || status
}

const getStatusType = (status: string) => {
  return statusMap[status]?.type || 'info'
}

/** 店铺列表 */
const shopList = ref([])

/** 查询列表 */
const loading = ref(true)
const total = ref(0)
const list = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  shopId: undefined,
  amazonOrderId: undefined,
  orderStatus: undefined,
  startTime: undefined,
  endTime: undefined
})
const queryFormRef = ref()
const dateRange = ref([])

const getList = async () => {
  loading.value = true
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      queryParams.startTime = dateRange.value[0]
      queryParams.endTime = dateRange.value[1]
    } else {
      queryParams.startTime = undefined
      queryParams.endTime = undefined
    }

    const data = await OrderApi.getOrderPage(queryParams)
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
  dateRange.value = []
  handleQuery()
}

/** 查看详情 */
const detailRef = ref()
const openDetail = (id: number) => {
  detailRef.value.open(id)
}

/** 同步订单 */
const handleSync = async () => {
  if (!queryParams.shopId) {
    message.warning('请先选择店铺')
    return
  }
  try {
    await OrderApi.syncOrders(queryParams.shopId)
    message.success('订单同步任务已启动')
    setTimeout(() => getList(), 2000)
  } catch (error) {
    message.error('同步失败')
  }
}

/** 发货 */
const handleShip = (row) => {
  message.info('发货功能开发中')
}

/** 初始化 */
onMounted(() => {
  loadShopList()
  getList()
})
</script>
