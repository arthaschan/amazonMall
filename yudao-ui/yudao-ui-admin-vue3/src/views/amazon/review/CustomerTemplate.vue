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
      <el-form-item label="模板名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入模板名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="模板类型" prop="type">
        <el-select
          v-model="queryParams.type"
          placeholder="全部类型"
          clearable
          class="!w-240px"
        >
          <el-option label="感谢模板" value="THANK_YOU" />
          <el-option label="道歉模板" value="APOLOGY" />
          <el-option label="跟进模板" value="FOLLOW_UP" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['amazon:review-template:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增模板
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 模板列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="模板名称" align="center" prop="name" min-width="150" />
      <el-table-column label="类型" align="center" prop="type" width="120">
        <template #default="scope">
          <el-tag :type="getTypeTag(scope.row.type)">
            {{ getTypeLabel(scope.row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="内容预览" align="left" prop="content" min-width="250" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="enabled" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.enabled ? 'success' : 'info'">
            {{ scope.row.enabled ? '启用' : '禁用' }}
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
          <el-button link type="primary" @click="handlePreview(scope.row)">
            预览
          </el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['amazon:review-template:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['amazon:review-template:delete']"
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
  <el-dialog v-model="formVisible" :title="formTitle" width="700px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
    >
      <el-form-item label="模板名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入模板名称" />
      </el-form-item>
      <el-form-item label="模板类型" prop="type">
        <el-select v-model="formData.type" placeholder="请选择类型" class="w-full">
          <el-option label="感谢模板" value="THANK_YOU" />
          <el-option label="道歉模板" value="APOLOGY" />
          <el-option label="跟进模板" value="FOLLOW_UP" />
        </el-select>
      </el-form-item>
      <el-form-item label="模板内容" prop="content">
        <el-input
          v-model="formData.content"
          type="textarea"
          :rows="8"
          placeholder="请输入模板内容，支持变量替换"
        />
      </el-form-item>
      <el-form-item label="可用变量">
        <div class="flex flex-wrap gap-5px">
          <el-tag
            v-for="v in variableList"
            :key="v.key"
            class="cursor-pointer"
            @click="insertVariable(v.key)"
          >
            {{ v.label }}: {{ '{{' + v.key + '}}' }}
          </el-tag>
        </div>
        <div class="text-gray-400 text-12px mt-5px">点击变量可插入到内容末尾</div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="formVisible = false">取 消</el-button>
    </template>
  </el-dialog>

  <!-- 预览弹窗 -->
  <el-dialog v-model="previewVisible" title="模板预览" width="600px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="模板名称">{{ previewData.name }}</el-descriptions-item>
      <el-descriptions-item label="模板类型">
        <el-tag :type="getTypeTag(previewData.type)">{{ getTypeLabel(previewData.type) }}</el-tag>
      </el-descriptions-item>
    </el-descriptions>
    <el-divider content-position="left">模板内容</el-divider>
    <div class="p-20px bg-gray-50 rounded border border-gray-200 whitespace-pre-wrap">
      {{ previewData.content }}
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { CustomerTemplateApi } from '@/api/amazon/review'

const message = useMessage()
const { t } = useI18n()

const typeMap = {
  'THANK_YOU': '感谢模板',
  'APOLOGY': '道歉模板',
  'FOLLOW_UP': '跟进模板'
}

const typeTagMap = {
  'THANK_YOU': 'success',
  'APOLOGY': 'warning',
  'FOLLOW_UP': 'primary'
}

const getTypeLabel = (type: string) => typeMap[type] || type
const getTypeTag = (type: string) => typeTagMap[type] || 'info'

/** 可用变量 */
const variableList = [
  { key: 'customerName', label: '客户姓名' },
  { key: 'productName', label: '商品名称' },
  { key: 'asin', label: 'ASIN' },
  { key: 'orderId', label: '订单号' },
  { key: 'shopName', label: '店铺名称' }
]

/** 查询列表 */
const loading = ref(true)
const total = ref(0)
const list = ref([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  type: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await CustomerTemplateApi.getCustomerTemplatePage(queryParams)
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
  type: undefined,
  content: undefined
})

const formRules = {
  name: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
  content: [{ required: true, message: '模板内容不能为空', trigger: 'blur' }]
}

const openForm = (type: string, id?: number) => {
  formVisible.value = true
  formTitle.value = type === 'create' ? '新增消息模板' : '修改消息模板'
  formType.value = type
  formRef.value?.resetFields()

  if (id) {
    formLoading.value = true
    // Load template detail when backend API is ready
    formLoading.value = false
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await CustomerTemplateApi.createCustomerTemplate(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await CustomerTemplateApi.updateCustomerTemplate(formData.value)
      message.success(t('common.updateSuccess'))
    }
    formVisible.value = false
    getList()
  } finally {
    formLoading.value = false
  }
}

/** 插入变量 */
const insertVariable = (key: string) => {
  formData.value.content = (formData.value.content || '') + `{{${key}}}`
}

/** 预览 */
const previewVisible = ref(false)
const previewData = ref<any>({})

const handlePreview = (row) => {
  previewData.value = row
  previewVisible.value = true
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await CustomerTemplateApi.deleteCustomerTemplate(id)
    message.success(t('common.delSuccess'))
    getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>
