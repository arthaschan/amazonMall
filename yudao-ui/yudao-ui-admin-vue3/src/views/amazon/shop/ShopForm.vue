<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
    >
      <el-form-item label="店铺名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入店铺名称" />
      </el-form-item>
      <el-form-item label="站点" prop="marketplaceId">
        <el-select v-model="formData.marketplaceId" placeholder="请选择站点" class="w-full">
          <el-option label="美国站" value="ATVPDKIKX0DER" />
          <el-option label="英国站" value="A1F83G8C2ARO7P" />
          <el-option label="德国站" value="A1PA6795UKMFR9" />
          <el-option label="日本站" value="A1VC38T7YXB528" />
        </el-select>
      </el-form-item>
      <el-form-item label="卖家ID" prop="sellerId">
        <el-input v-model="formData.sellerId" placeholder="请输入卖家ID" />
      </el-form-item>
      <el-form-item label="MWS Auth Token" prop="mwsAuthToken">
        <el-input
          v-model="formData.mwsAuthToken"
          type="password"
          show-password
          placeholder="请输入 MWS Auth Token"
        />
      </el-form-item>
      <el-form-item label="AWS Access Key" prop="awsAccessKey">
        <el-input
          v-model="formData.awsAccessKey"
          type="password"
          show-password
          placeholder="请输入 AWS Access Key"
        />
      </el-form-item>
      <el-form-item label="AWS Secret Key" prop="awsSecretKey">
        <el-input
          v-model="formData.awsSecretKey"
          type="password"
          show-password
          placeholder="请输入 AWS Secret Key"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ShopApi } from '@/api/amazon/shop'

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()

const formData = ref({
  id: undefined,
  name: undefined,
  marketplaceId: undefined,
  sellerId: undefined,
  mwsAuthToken: undefined,
  awsAccessKey: undefined,
  awsSecretKey: undefined,
  status: 1,
  remark: undefined
})

const formRules = {
  name: [{ required: true, message: '店铺名称不能为空', trigger: 'blur' }],
  marketplaceId: [{ required: true, message: '站点不能为空', trigger: 'change' }],
  sellerId: [{ required: true, message: '卖家ID不能为空', trigger: 'blur' }],
  mwsAuthToken: [{ required: true, message: 'MWS Auth Token不能为空', trigger: 'blur' }],
  awsAccessKey: [{ required: true, message: 'AWS Access Key不能为空', trigger: 'blur' }],
  awsSecretKey: [{ required: true, message: 'AWS Secret Key不能为空', trigger: 'blur' }]
}

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '添加店铺' : '修改店铺'
  formType.value = type
  formRef.value?.resetFields()

  if (id) {
    formLoading.value = true
    try {
      formData.value = await ShopApi.getShop(id)
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
      await ShopApi.createShop(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await ShopApi.updateShop(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })
</script>
