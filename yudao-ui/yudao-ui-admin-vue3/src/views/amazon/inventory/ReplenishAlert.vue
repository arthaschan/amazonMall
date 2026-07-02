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
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="全部状态"
          clearable
          class="!w-240px"
        >
          <el-option label="待处理" value="PENDING" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['amazon:replenish-alert:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新建预警
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="SKU" align="center" prop="sku" width="150" />
      <el-table-column label="商品名称" align="left" prop="title" min-width="200" show-overflow-tooltip />
      <el-table-column label="店铺" align="center" prop="shopName" width="120" />
      <el-table-column label="当前库存" align="center" prop="currentStock" width="100">
        <template #default="scope">
          <span :class="{ 'text-danger font-bold': scope.row.currentStock <= scope.row.reorderPoint }">
            {{ scope.row.currentStock }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="补货点" align="center" prop="reorderPoint" width="100" />
      <el-table-column label="建议补货量" align="center" prop="suggestedQuantity" width="120">
        <template #default="scope">
          <span class="font-bold text-primary">{{ scope.row.suggestedQuantity }}</span>
        </template>
      </el-table-column>
      <el-table-column label="供货周期" align="center" prop="leadTime" width="100">
        <template #default="scope">
          {{ scope.row.leadTime }} 天
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['amazon:replenish-alert:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="success"
            @click="handleConfirm(scope.row)"
            v-if="scope.row.status === 'PENDING'"
          >
            确认
          </el-button>
          <el-button
            link
            type="warning"
            @click="handleCancel(scope.row)"
            v-if="scope.row.status === 'PENDING'"
          >
            取消
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['amazon:replenish-alert:delete']"
          >
            删除
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

  <!-- 表单弹窗：添加/修改 -->
  <el-dialog v-model="formVisible" :title="formTitle" width="600px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
    >
      <el-form-item label="店铺" prop="shopId">
        <el-select v-model="formData.shopId" placeholder="请选择店铺" class="w-full">
          <el-option v-for="shop in shopList" :key="shop.id" :label="shop.name" :value="shop.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="SKU" prop="sku">
        <el-input v-model="formData.sku" placeholder="请输入SKU" :disabled="formType === 'update'" />
      </el-form-item>
      <el-form-item label="补货点" prop="reorderPoint">
        <el-input-number v-model="formData.reorderPoint" :min="0" class="!w-full" placeholder="库存低于此值时触发预警" />
      </el-form-item>
      <el-form-item label="建议补货量" prop="suggestedQuantity">
        <el-input-number v-model="formData.suggestedQuantity" :min="1" class="!w-full" />
      </el-form-item>
      <el-form-item label="供货周期(天)" prop="leadTime">
        <el-input-number v-model="formData.leadTime" :min="1" :max="365" class="!w-full" placeholder="从下单到入库的天数" />
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
import { ReplenishAlertApi } from '@/api/amazon/inventory'
import { ShopApi } from '@/api/amazon/shop'

const message = useMessage()
const { t } = useI18n()

const statusMap = {
  'PENDING': { label: '待处理', type: 'warning' },
  'CONFIRMED': { label: '已确认', type: 'success' },
  'CANCELLED': { label: '已取消', type: 'info' }
}

const getStatusLabel = (status: string) => statusMap[status]?.label || status
const getStatusType = (status: string) => statusMap[status]?.type || 'info'

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
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ReplenishAlertApi.getReplenishAlertPage(queryParams)
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

/** 表单 */
const formVisible = ref(false)
const formTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref({
  id: undefined,
  shopId: undefined,
  sku: undefined,
  reorderPoint: undefined,
  suggestedQuantity: undefined,
  leadTime: undefined,
  remark: undefined
})

const formRules = {
  shopId: [{ required: true, message: '店铺不能为空', trigger: 'change' }],
  sku: [{ required: true, message: 'SKU不能为空', trigger: 'blur' }],
  reorderPoint: [{ required: true, message: '补货点不能为空', trigger: 'blur' }],
  suggestedQuantity: [{ required: true, message: '建议补货量不能为空', trigger: 'blur' }],
  leadTime: [{ required: true, message: '供货周期不能为空', trigger: 'blur' }]
}

const openForm = async (type: string, id?: number) => {
  formVisible.value = true
  formTitle.value = type === 'create' ? '新建补货预警' : '编辑补货预警'
  formType.value = type
  formRef.value?.resetFields()
  formData.value = {
    id: undefined,
    shopId: undefined,
    sku: undefined,
    reorderPoint: undefined,
    suggestedQuantity: undefined,
    leadTime: undefined,
    remark: undefined
  }

  if (id) {
    formLoading.value = true
    try {
      const data = await ReplenishAlertApi.getReplenishAlertPage({ id, pageNo: 1, pageSize: 1 })
      if (data.list && data.list.length > 0) {
        formData.value = { ...data.list[0] }
      }
    } finally {
      formLoading.value = false
    }
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await ReplenishAlertApi.createReplenishAlert(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await ReplenishAlertApi.updateReplenishAlert(formData.value)
      message.success(t('common.updateSuccess'))
    }
    formVisible.value = false
    getList()
  } finally {
    formLoading.value = false
  }
}

/** 确认预警 */
const handleConfirm = async (row) => {
  try {
    await message.confirm('确定要确认此补货预警吗？')
    await ReplenishAlertApi.updateReplenishAlert({ id: row.id, status: 'CONFIRMED' })
    message.success('确认成功')
    getList()
  } catch {}
}

/** 取消预警 */
const handleCancel = async (row) => {
  try {
    await message.confirm('确定要取消此补货预警吗？')
    await ReplenishAlertApi.updateReplenishAlert({ id: row.id, status: 'CANCELLED' })
    message.success('取消成功')
    getList()
  } catch {}
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await ReplenishAlertApi.deleteReplenishAlert(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  loadShopList()
  getList()
})
</script>
