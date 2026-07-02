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
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['amazon:ad-rule:create']"
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
      <el-table-column label="条件" align="center" prop="conditionType" width="160">
        <template #default="scope">
          {{ getConditionLabel(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="执行动作" align="center" prop="action" width="160">
        <template #default="scope">
          <el-tag :type="getActionTag(scope.row.action)">
            {{ getActionLabel(scope.row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="enabled" width="100">
        <template #default="scope">
          <el-switch
            v-model="scope.row.enabled"
            @change="handleToggle(scope.row)"
            v-hasPermi="['amazon:ad-rule:update']"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="最后触发时间"
        align="center"
        prop="lastTriggeredTime"
        :formatter="dateFormatter"
        width="180"
      />
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
            v-hasPermi="['amazon:ad-rule:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['amazon:ad-rule:delete']"
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
      <el-form-item label="条件类型" prop="conditionType">
        <el-select v-model="formData.conditionType" placeholder="请选择条件" class="w-full">
          <el-option label="ACoS" value="ACOS" />
          <el-option label="花费" value="SPEND" />
          <el-option label="点击数" value="CLICKS" />
        </el-select>
      </el-form-item>
      <el-form-item label="运算符" prop="operator">
        <el-select v-model="formData.operator" placeholder="请选择运算符" class="w-full">
          <el-option label="大于 (>)" value="GT" />
          <el-option label="小于 (<)" value="LT" />
          <el-option label="等于 (=)" value="EQ" />
          <el-option label="大于等于 (>=)" value="GTE" />
          <el-option label="小于等于 (<=)" value="LTE" />
        </el-select>
      </el-form-item>
      <el-form-item label="阈值" prop="threshold">
        <el-input-number
          v-model="formData.threshold"
          :min="0"
          :precision="2"
          :step="1"
          class="!w-full"
        />
      </el-form-item>
      <el-form-item label="执行动作" prop="action">
        <el-select v-model="formData.action" placeholder="请选择动作" class="w-full">
          <el-option label="提高竞价" value="INCREASE_BID" />
          <el-option label="降低竞价" value="DECREASE_BID" />
          <el-option label="暂停广告" value="PAUSE" />
        </el-select>
      </el-form-item>
      <el-form-item label="动作值" prop="actionValue">
        <el-input-number
          v-model="formData.actionValue"
          :min="0"
          :precision="2"
          :step="0.01"
          class="!w-full"
        />
        <div class="text-gray-400 text-12px mt-5px">
          提高/降低竞价时为金额($)，暂停时无需填写
        </div>
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
import { AdRuleApi } from '@/api/amazon/ad'

const message = useMessage()
const { t } = useI18n()

/** 条件标签映射 */
const conditionMap = {
  'ACOS': 'ACoS',
  'SPEND': '花费',
  'CLICKS': '点击数'
}

const operatorMap = {
  'GT': '>',
  'LT': '<',
  'EQ': '=',
  'GTE': '>=',
  'LTE': '<='
}

const actionMap = {
  'INCREASE_BID': '提高竞价',
  'DECREASE_BID': '降低竞价',
  'PAUSE': '暂停广告'
}

const getConditionLabel = (row) => {
  const cond = conditionMap[row.conditionType] || row.conditionType
  const op = operatorMap[row.operator] || row.operator
  return `${cond} ${op} ${row.threshold}`
}

const getActionLabel = (row) => {
  const action = actionMap[row.action] || row.action
  if (row.action === 'PAUSE') return action
  return `${action} $${row.actionValue || 0}`
}

const getActionTag = (action: string) => {
  const map = { 'INCREASE_BID': 'success', 'DECREASE_BID': 'warning', 'PAUSE': 'danger' }
  return map[action] || 'info'
}

/** 查询列表 */
const loading = ref(true)
const total = ref(0)
const list = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await AdRuleApi.getAdRulePage(queryParams)
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

const formData = ref({
  id: undefined,
  name: undefined,
  conditionType: undefined,
  operator: undefined,
  threshold: undefined,
  action: undefined,
  actionValue: undefined
})

const formRules = {
  name: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }],
  conditionType: [{ required: true, message: '请选择条件类型', trigger: 'change' }],
  operator: [{ required: true, message: '请选择运算符', trigger: 'change' }],
  threshold: [{ required: true, message: '阈值不能为空', trigger: 'blur' }],
  action: [{ required: true, message: '请选择执行动作', trigger: 'change' }]
}

const openForm = (type: string, id?: number) => {
  formVisible.value = true
  formTitle.value = type === 'create' ? '新增广告规则' : '修改广告规则'
  formType.value = type
  formRef.value?.resetFields()

  if (id) {
    formLoading.value = true
    // Load rule detail when backend API is ready
    formLoading.value = false
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await AdRuleApi.createAdRule(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await AdRuleApi.updateAdRule(formData.value)
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
    await AdRuleApi.updateAdRule({ id: row.id, enabled: row.enabled })
    message.success('操作成功')
  } catch (error) {
    row.enabled = !row.enabled
  }
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AdRuleApi.deleteAdRule(id)
    message.success(t('common.delSuccess'))
    getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>
