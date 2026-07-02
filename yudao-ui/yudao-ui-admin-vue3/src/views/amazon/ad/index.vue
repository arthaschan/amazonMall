<template>
  <!-- 汇总卡片 -->
  <el-row :gutter="20" class="mb-20px">
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-danger">${{ summary.totalSpend || '0.00' }}</div>
          <div class="text-gray-500 mt-5px">总花费</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-success">${{ summary.totalSales || '0.00' }}</div>
          <div class="text-gray-500 mt-5px">总销售额</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-warning">{{ summary.avgACoS || '0.00' }}%</div>
          <div class="text-gray-500 mt-5px">平均ACoS</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="text-center">
          <div class="text-28px font-bold text-primary">{{ summary.totalImpressions || 0 }}</div>
          <div class="text-gray-500 mt-5px">总曝光量</div>
        </div>
      </el-card>
    </el-col>
  </el-row>

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
      <el-form-item label="广告类型" prop="campaignType">
        <el-select
          v-model="queryParams.campaignType"
          placeholder="全部类型"
          clearable
          class="!w-240px"
        >
          <el-option label="SP - 商品推广" value="SP" />
          <el-option label="SB - 品牌推广" value="SB" />
          <el-option label="SD - 展示推广" value="SD" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="state">
        <el-select
          v-model="queryParams.state"
          placeholder="全部状态"
          clearable
          class="!w-240px"
        >
          <el-option label="启用" value="ENABLED" />
          <el-option label="暂停" value="PAUSED" />
          <el-option label="归档" value="ARCHIVED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['amazon:ad:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增活动
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 广告活动列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="活动名称" align="left" prop="campaignName" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" align="center" prop="campaignType" width="100">
        <template #default="scope">
          <el-tag :type="getCampaignTypeTag(scope.row.campaignType)">
            {{ scope.row.campaignType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="state" width="100">
        <template #default="scope">
          <el-switch
            v-model="scope.row.state"
            active-value="ENABLED"
            inactive-value="PAUSED"
            @change="handleToggle(scope.row)"
            v-hasPermi="['amazon:ad:update']"
          />
        </template>
      </el-table-column>
      <el-table-column label="每日预算" align="center" prop="dailyBudget" width="110">
        <template #default="scope">
          ${{ scope.row.dailyBudget || '0.00' }}
        </template>
      </el-table-column>
      <el-table-column label="花费" align="center" prop="spend" width="110">
        <template #default="scope">
          ${{ scope.row.spend || '0.00' }}
        </template>
      </el-table-column>
      <el-table-column label="销售额" align="center" prop="sales" width="110">
        <template #default="scope">
          ${{ scope.row.sales || '0.00' }}
        </template>
      </el-table-column>
      <el-table-column label="ACoS" align="center" prop="acos" width="90">
        <template #default="scope">
          <span :class="scope.row.acos > 30 ? 'text-danger' : 'text-success'">
            {{ scope.row.acos || '0.00' }}%
          </span>
        </template>
      </el-table-column>
      <el-table-column label="曝光" align="center" prop="impressions" width="100" />
      <el-table-column label="点击" align="center" prop="clicks" width="80" />
      <el-table-column label="CTR" align="center" prop="ctr" width="80">
        <template #default="scope">
          {{ scope.row.ctr || '0.00' }}%
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['amazon:ad:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['amazon:ad:delete']"
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
      <el-form-item label="活动名称" prop="campaignName">
        <el-input v-model="formData.campaignName" placeholder="请输入活动名称" />
      </el-form-item>
      <el-form-item label="店铺" prop="shopId">
        <el-select v-model="formData.shopId" placeholder="请选择店铺" class="w-full">
          <el-option
            v-for="shop in shopList"
            :key="shop.id"
            :label="shop.name"
            :value="shop.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="广告类型" prop="campaignType">
        <el-select v-model="formData.campaignType" placeholder="请选择广告类型" class="w-full">
          <el-option label="SP - 商品推广" value="SP" />
          <el-option label="SB - 品牌推广" value="SB" />
          <el-option label="SD - 展示推广" value="SD" />
        </el-select>
      </el-form-item>
      <el-form-item label="每日预算" prop="dailyBudget">
        <el-input-number
          v-model="formData.dailyBudget"
          :min="1"
          :precision="2"
          :step="1"
          class="!w-full"
        />
      </el-form-item>
      <el-form-item label="默认竞价" prop="defaultBid">
        <el-input-number
          v-model="formData.defaultBid"
          :min="0.01"
          :precision="2"
          :step="0.01"
          class="!w-full"
        />
      </el-form-item>
      <el-form-item label="状态" prop="state">
        <el-radio-group v-model="formData.state">
          <el-radio label="ENABLED">启用</el-radio>
          <el-radio label="PAUSED">暂停</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="formVisible = false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { AdCampaignApi } from '@/api/amazon/ad'
import { ShopApi } from '@/api/amazon/shop'

const message = useMessage()
const { t } = useI18n()

/** 店铺列表 */
const shopList = ref([])

/** 汇总数据 */
const summary = ref({
  totalSpend: '0.00',
  totalSales: '0.00',
  avgACoS: '0.00',
  totalImpressions: 0
})

/** 广告类型标签映射 */
const getCampaignTypeTag = (type: string) => {
  const map = { 'SP': 'success', 'SB': 'primary', 'SD': 'warning' }
  return map[type] || 'info'
}

/** 查询列表 */
const loading = ref(true)
const total = ref(0)
const list = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  shopId: undefined,
  campaignType: undefined,
  state: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await AdCampaignApi.getAdCampaignPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 加载汇总 */
const loadSummary = async () => {
  try {
    summary.value = await AdCampaignApi.getAdCampaignPage({
      ...queryParams,
      pageNo: 1,
      pageSize: 1,
      _summary: true
    })
  } catch (error) {
    console.error('加载汇总数据失败', error)
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
  loadSummary()
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
  campaignName: undefined,
  shopId: undefined,
  campaignType: undefined,
  dailyBudget: undefined,
  defaultBid: undefined,
  state: 'ENABLED'
})

const formRules = {
  campaignName: [{ required: true, message: '活动名称不能为空', trigger: 'blur' }],
  shopId: [{ required: true, message: '请选择店铺', trigger: 'change' }],
  campaignType: [{ required: true, message: '请选择广告类型', trigger: 'change' }],
  dailyBudget: [{ required: true, message: '每日预算不能为空', trigger: 'blur' }]
}

const openForm = async (type: string, id?: number) => {
  formVisible.value = true
  formTitle.value = type === 'create' ? '新增广告活动' : '修改广告活动'
  formType.value = type
  formRef.value?.resetFields()

  if (id) {
    formLoading.value = true
    try {
      formData.value = await AdCampaignApi.getAdCampaign(id)
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
      await AdCampaignApi.createAdCampaign(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await AdCampaignApi.updateAdCampaign(formData.value)
      message.success(t('common.updateSuccess'))
    }
    formVisible.value = false
    getList()
    loadSummary()
  } finally {
    formLoading.value = false
  }
}

/** 启用/暂停切换 */
const handleToggle = async (row) => {
  try {
    await AdCampaignApi.toggleCampaign(row.id, row.state)
    message.success('操作成功')
  } catch (error) {
    row.state = row.state === 'ENABLED' ? 'PAUSED' : 'ENABLED'
  }
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AdCampaignApi.deleteAdCampaign(id)
    message.success(t('common.delSuccess'))
    getList()
    loadSummary()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  loadShopList()
  getList()
  loadSummary()
})
</script>
