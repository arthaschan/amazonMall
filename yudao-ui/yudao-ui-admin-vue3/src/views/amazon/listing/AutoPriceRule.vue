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
      <el-form-item label="规则名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入规则名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="enabled">
        <el-select
          v-model="queryParams.enabled"
          placeholder="全部状态"
          clearable
          class="!w-240px"
        >
          <el-option label="启用" :value="true" />
          <el-option label="禁用" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['amazon:auto-price:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增规则
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 规则列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="规则名称" align="center" prop="name" min-width="150" />
      <el-table-column label="策略类型" align="center" prop="strategyType" width="120">
        <template #default="scope">
          <el-tag>{{ getStrategyLabel(scope.row.strategyType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目标价格" align="center" prop="targetPrice" width="120">
        <template #default="scope">
          ${{ scope.row.targetPrice }}
        </template>
      </el-table-column>
      <el-table-column label="最低价" align="center" prop="minPrice" width="100">
        <template #default="scope">
          ${{ scope.row.minPrice }}
        </template>
      </el-table-column>
      <el-table-column label="最高价" align="center" prop="maxPrice" width="100">
        <template #default="scope">
          ${{ scope.row.maxPrice }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="enabled" width="100">
        <template #default="scope">
          <el-switch
            v-model="scope.row.enabled"
            @change="handleToggle(scope.row)"
            v-hasPermi="['amazon:auto-price:update']"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['amazon:auto-price:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['amazon:auto-price:delete']"
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

  <!-- 表单弹窗 -->
  <el-dialog v-model="formVisible" :title="formTitle" width="600px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
    >
      <el-form-item label="规则名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入规则名称" />
      </el-form-item>
      <el-form-item label="策略类型" prop="strategyType">
        <el-select v-model="formData.strategyType" placeholder="请选择策略" class="w-full">
          <el-option label="竞争Buy Box" value="BUY_BOX" />
          <el-option label="固定价格" value="FIXED" />
          <el-option label="动态调整" value="DYNAMIC" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标价格" prop="targetPrice">
        <el-input-number
          v-model="formData.targetPrice"
          :min="0"
          :precision="2"
          :step="0.01"
          class="!w-full"
        />
      </el-form-item>
      <el-form-item label="价格区间" prop="minPrice">
        <el-col :span="11">
          <el-input-number
            v-model="formData.minPrice"
            :min="0"
            :precision="2"
            :step="0.01"
            class="!w-full"
            placeholder="最低价"
          />
        </el-col>
        <el-col :span="2" class="text-center">-</el-col>
        <el-col :span="11">
          <el-input-number
            v-model="formData.maxPrice"
            :min="0"
            :precision="2"
            :step="0.01"
            class="!w-full"
            placeholder="最高价"
          />
        </el-col>
      </el-form-item>
      <el-form-item label="应用商品" prop="productIds">
        <el-select
          v-model="formData.productIds"
          multiple
          filterable
          placeholder="请选择商品（不选则应用全部）"
          class="w-full"
        >
          <el-option
            v-for="product in productList"
            :key="product.id"
            :label="`${product.sku} - ${product.title}`"
            :value="product.id"
          />
        </el-select>
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
import { AutoPriceApi, ProductApi } from '@/api/amazon/listing'

const message = useMessage()
const { t } = useI18n()

const strategyMap = {
  'BUY_BOX': '竞争Buy Box',
  'FIXED': '固定价格',
  'DYNAMIC': '动态调整'
}

const getStrategyLabel = (type: string) => {
  return strategyMap[type] || type
}

/** 查询列表 */
const loading = ref(true)
const total = ref(0)
const list = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  enabled: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await AutoPriceApi.getRulePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
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
const productList = ref([])

const formData = ref({
  id: undefined,
  name: undefined,
  strategyType: undefined,
  targetPrice: undefined,
  minPrice: undefined,
  maxPrice: undefined,
  productIds: []
})

const formRules = {
  name: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }],
  strategyType: [{ required: true, message: '策略类型不能为空', trigger: 'change' }],
  targetPrice: [{ required: true, message: '目标价格不能为空', trigger: 'blur' }]
}

const loadProductList = async () => {
  try {
    const data = await ProductApi.getProductPage({ pageNo: 1, pageSize: 1000 })
    productList.value = data.list
  } catch (error) {
    console.error('加载商品列表失败', error)
  }
}

const openForm = (type: string, id?: number) => {
  formVisible.value = true
  formTitle.value = type === 'create' ? '添加定价规则' : '修改定价规则'
  formType.value = type
  formRef.value?.resetFields()
  loadProductList()

  if (id) {
    formLoading.value = true
    // TODO: Load rule detail
    formLoading.value = false
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await AutoPriceApi.createRule(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await AutoPriceApi.updateRule(formData.value)
      message.success(t('common.updateSuccess'))
    }
    formVisible.value = false
    getList()
  } finally {
    formLoading.value = false
  }
}

/** 启用/禁用 */
const handleToggle = async (row) => {
  try {
    await AutoPriceApi.toggleRule(row.id, row.enabled)
    message.success('操作成功')
  } catch (error) {
    row.enabled = !row.enabled
  }
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AutoPriceApi.deleteRule(id)
    message.success(t('common.delSuccess'))
    getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>
