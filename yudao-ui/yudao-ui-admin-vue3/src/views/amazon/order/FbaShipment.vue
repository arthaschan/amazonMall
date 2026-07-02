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
        <el-select v-model="queryParams.shopId" placeholder="全部店铺" clearable class="!w-240px">
          <el-option v-for="shop in shopList" :key="shop.id" :label="shop.name" :value="shop.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-240px">
          <el-option label="待发货" value="PENDING" />
          <el-option label="已发货" value="SHIPPED" />
          <el-option label="已接收" value="RECEIVED" />
          <el-option label="已完成" value="COMPLETED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['amazon:fba-shipment:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新建发货计划
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 发货计划列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="发货编号" align="center" prop="shipmentId" width="180" />
      <el-table-column label="店铺" align="center" prop="shopName" width="120" />
      <el-table-column label="FBA仓库" align="center" prop="destinationFulfillmentCenterId" width="150" />
      <el-table-column label="商品数量" align="center" prop="totalUnits" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
          <el-button
            link
            type="success"
            @click="handleConfirm(scope.row)"
            v-if="scope.row.status === 'PENDING'"
          >
            确认发货
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-if="scope.row.status === 'PENDING'"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 新建发货计划弹窗 -->
  <el-dialog v-model="formVisible" :title="formTitle" width="700px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" v-loading="formLoading">
      <el-form-item label="店铺" prop="shopId">
        <el-select v-model="formData.shopId" placeholder="请选择店铺" class="w-full">
          <el-option v-for="shop in shopList" :key="shop.id" :label="shop.name" :value="shop.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="发货商品" prop="items">
        <el-table :data="formData.items" border>
          <el-table-column label="SKU" align="center" prop="sku" width="150" />
          <el-table-column label="商品名称" align="left" prop="title" min-width="200" />
          <el-table-column label="发货数量" align="center" width="150">
            <template #default="scope">
              <el-input-number v-model="scope.row.quantity" :min="1" class="!w-full" />
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="80">
            <template #default="scope">
              <el-button link type="danger" @click="removeItem(scope.$index)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button type="primary" link @click="addItem" class="mt-10px">+ 添加商品</el-button>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="formVisible = false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { FbaShipmentApi } from '@/api/amazon/order'
import { ShopApi } from '@/api/amazon/shop'

const message = useMessage()
const { t } = useI18n()

const statusMap = {
  'PENDING': { label: '待发货', type: 'warning' },
  'SHIPPED': { label: '已发货', type: 'primary' },
  'RECEIVED': { label: '已接收', type: 'success' },
  'COMPLETED': { label: '已完成', type: 'success' }
}

const getStatusLabel = (status: string) => statusMap[status]?.label || status
const getStatusType = (status: string) => statusMap[status]?.type || 'info'

const shopList = ref([])
const loading = ref(true)
const total = ref(0)
const list = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  shopId: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await FbaShipmentApi.getShipmentPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const loadShopList = async () => {
  try {
    const data = await ShopApi.getShopPage({ pageNo: 1, pageSize: 100 })
    shopList.value = data.list
  } catch (error) {
    console.error('加载店铺列表失败', error)
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const formVisible = ref(false)
const formTitle = ref('')
const formLoading = ref(false)
const formRef = ref()
const formData = ref({
  shopId: undefined,
  items: [],
  remark: undefined
})
const formRules = {
  shopId: [{ required: true, message: '店铺不能为空', trigger: 'change' }]
}

const openForm = (type: string) => {
  formVisible.value = true
  formTitle.value = '新建发货计划'
  loadShopList()
  formData.value = { shopId: undefined, items: [], remark: undefined }
}

const addItem = () => {
  formData.value.items.push({ sku: '', title: '', quantity: 1 })
}

const removeItem = (index: number) => {
  formData.value.items.splice(index, 1)
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    await FbaShipmentApi.createShipment(formData.value)
    message.success(t('common.createSuccess'))
    formVisible.value = false
    getList()
  } finally {
    formLoading.value = false
  }
}

const openDetail = (row) => {
  message.info('详情功能开发中')
}

const handleConfirm = async (row) => {
  try {
    await message.confirm('确定要确认发货吗？')
    await FbaShipmentApi.confirmShipment(row.id)
    message.success('确认发货成功')
    getList()
  } catch {}
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await FbaShipmentApi.deleteShipment(id)
    message.success(t('common.delSuccess'))
    getList()
  } catch {}
}

onMounted(() => {
  loadShopList()
  getList()
})
</script>
