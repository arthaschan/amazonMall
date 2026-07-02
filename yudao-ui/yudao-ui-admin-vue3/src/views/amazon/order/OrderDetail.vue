<template>
  <el-dialog v-model="dialogVisible" title="订单详情" width="900px" v-loading="loading">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="亚马逊订单号" :span="2">
        {{ data.amazonOrderId }}
      </el-descriptions-item>
      <el-descriptions-item label="店铺">{{ data.shopName }}</el-descriptions-item>
      <el-descriptions-item label="订单状态">
        <el-tag :type="getStatusType(data.orderStatus)">
          {{ getStatusLabel(data.orderStatus) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="订单金额">${{ data.orderTotal }}</el-descriptions-item>
      <el-descriptions-item label="下单时间">
        {{ dateFormatter(data.purchaseDate) }}
      </el-descriptions-item>
      <el-descriptions-item label="配送方式">
        <el-tag :type="data.fulfillmentChannel === 'AFN' ? 'success' : 'info'">
          {{ data.fulfillmentChannel === 'AFN' ? 'FBA' : 'FBM' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="支付方式">{{ data.paymentMethod }}</el-descriptions-item>
    </el-descriptions>

    <!-- 收货地址 -->
    <h4 class="font-bold mt-20px mb-10px">收货地址</h4>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="收件人" :span="2">{{ data.shippingAddress?.name }}</el-descriptions-item>
      <el-descriptions-item label="电话">{{ data.shippingAddress?.phone }}</el-descriptions-item>
      <el-descriptions-item label="邮编">{{ data.shippingAddress?.postalCode }}</el-descriptions-item>
      <el-descriptions-item label="地址" :span="2">
        {{ formatAddress(data.shippingAddress) }}
      </el-descriptions-item>
    </el-descriptions>

    <!-- 商品明细 -->
    <h4 class="font-bold mt-20px mb-10px">商品明细</h4>
    <el-table :data="data.items" border>
      <el-table-column label="SKU" align="center" prop="sku" width="150" />
      <el-table-column label="商品名称" align="left" prop="title" min-width="200" />
      <el-table-column label="数量" align="center" prop="quantityOrdered" width="100" />
      <el-table-column label="单价" align="center" prop="itemPrice" width="120">
        <template #default="scope">
          ${{ scope.row.itemPrice }}
        </template>
      </el-table-column>
      <el-table-column label="小计" align="center" width="120">
        <template #default="scope">
          ${{ (scope.row.itemPrice * scope.row.quantityOrdered).toFixed(2) }}
        </template>
      </el-table-column>
    </el-table>

    <!-- 物流信息 -->
    <h4 class="font-bold mt-20px mb-10px" v-if="data.trackingNumber">物流信息</h4>
    <el-descriptions :column="2" border v-if="data.trackingNumber">
      <el-descriptions-item label="物流单号">{{ data.trackingNumber }}</el-descriptions-item>
      <el-descriptions-item label="物流公司">{{ data.carrierName }}</el-descriptions-item>
      <el-descriptions-item label="发货时间">
        {{ dateFormatter(data.shippedDate) }}
      </el-descriptions-item>
      <el-descriptions-item label="预计送达">
        {{ dateFormatter(data.estimatedDeliveryDate) }}
      </el-descriptions-item>
    </el-descriptions>

    <template #footer>
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { OrderApi } from '@/api/amazon/order'

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

const formatAddress = (address) => {
  if (!address) return '-'
  return `${address.addressLine1 || ''} ${address.addressLine2 || ''} ${address.city || ''} ${address.stateOrRegion || ''} ${address.countryCode || ''}`
}

const dialogVisible = ref(false)
const loading = ref(false)
const data = ref({})

const open = async (id: number) => {
  dialogVisible.value = true
  loading.value = true
  try {
    data.value = await OrderApi.getOrder(id)
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
