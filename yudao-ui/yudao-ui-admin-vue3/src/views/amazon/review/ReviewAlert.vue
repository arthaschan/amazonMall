<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="预警名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入预警名称"
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
          v-hasPermi="['amazon:review-alert:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增预警
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 预警规则列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="预警名称" align="center" prop="name" min-width="150" />
      <el-table-column label="触发条件" align="center" prop="starThreshold" width="180">
        <template #default="scope">
          星级 &le; {{ scope.row.starThreshold }} 星
        </template>
      </el-table-column>
      <el-table-column label="通知方式" align="center" prop="notifyMethod" width="150">
        <template #default="scope">
          <el-tag>{{ getNotifyLabel(scope.row.notifyMethod) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="enabled" width="100">
        <template #default="scope">
          <el-switch
            v-model="scope.row.enabled"
            @change="handleToggle(scope.row)"
            v-hasPermi="['amazon:review-alert:update']"
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
            v-hasPermi="['amazon:review-alert:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['amazon:review-alert:delete']"
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
      <el-form-item label="预警名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入预警名称" />
      </el-form-item>
      <el-form-item label="触发星级" prop="starThreshold">
        <el-select v-model="formData.starThreshold" placeholder="请选择星级阈值" class="w-full">
          <el-option label="1星及以下" :value="1" />
          <el-option label="2星及以下" :value="2" />
          <el-option label="3星及以下" :value="3" />
          <el-option label="4星及以下" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="通知方式" prop="notifyMethod">
        <el-select v-model="formData.notifyMethod" placeholder="请选择通知方式" class="w-full">
          <el-option label="站内通知" value="IN_APP" />
          <el-option label="邮件通知" value="EMAIL" />
          <el-option label="钉钉通知" value="DINGTALK" />
          <el-option label="企业微信" value="WECHAT" />
        </el-select>
      </el-form-item>
      <el-form-item label="应用ASIN" prop="asins">
        <el-input
          v-model="formData.asins"
          type="textarea"
          :rows="3"
          placeholder="输入ASIN（多个用逗号分隔，留空则应用全部）"
        />
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
import { ReviewAlertApi } from '@/api/amazon/review'

const message = useMessage()
const { t } = useI18n()

const notifyMap = {
  'IN_APP': '站内通知',
  'EMAIL': '邮件通知',
  'DINGTALK': '钉钉通知',
  'WECHAT': '企业微信'
}

const getNotifyLabel = (method: string) => notifyMap[method] || method

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
    const data = await ReviewAlertApi.getReviewAlertPage(queryParams)
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
  starThreshold: undefined,
  notifyMethod: undefined,
  asins: undefined
})

const formRules = {
  name: [{ required: true, message: '预警名称不能为空', trigger: 'blur' }],
  starThreshold: [{ required: true, message: '请选择触发星级', trigger: 'change' }],
  notifyMethod: [{ required: true, message: '请选择通知方式', trigger: 'change' }]
}

const openForm = (type: string, id?: number) => {
  formVisible.value = true
  formTitle.value = type === 'create' ? '新增评论预警' : '修改评论预警'
  formType.value = type
  formRef.value?.resetFields()

  if (id) {
    formLoading.value = true
    // Load alert detail when backend API is ready
    formLoading.value = false
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await ReviewAlertApi.createReviewAlert(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await ReviewAlertApi.updateReviewAlert(formData.value)
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
    await ReviewAlertApi.updateReviewAlert({ id: row.id, enabled: row.enabled })
    message.success('操作成功')
  } catch (error) {
    row.enabled = !row.enabled
  }
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await ReviewAlertApi.deleteReviewAlert(id)
    message.success(t('common.delSuccess'))
    getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>
