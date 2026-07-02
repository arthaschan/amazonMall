<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="800px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
    >
      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
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
          <el-form-item label="ASIN" prop="asin">
            <el-input v-model="formData.asin" placeholder="请输入ASIN" />
          </el-form-item>
          <el-form-item label="SKU" prop="sku">
            <el-input v-model="formData.sku" placeholder="请输入SKU" />
          </el-form-item>
          <el-form-item label="商品标题" prop="title">
            <el-input v-model="formData.title" placeholder="请输入商品标题" />
          </el-form-item>
          <el-form-item label="品牌" prop="brand">
            <el-input v-model="formData.brand" placeholder="请输入品牌" />
          </el-form-item>
          <el-form-item label="价格" prop="price">
            <el-input-number
              v-model="formData.price"
              :min="0"
              :precision="2"
              :step="0.01"
              class="!w-full"
            />
          </el-form-item>
          <el-form-item label="库存" prop="quantity">
            <el-input-number v-model="formData.quantity" :min="0" class="!w-full" />
          </el-form-item>
        </el-tab-pane>

        <!-- 详情描述 -->
        <el-tab-pane label="详情描述" name="description">
          <el-form-item label="五点描述" prop="bulletPoints">
            <el-input
              v-model="formData.bulletPoints"
              type="textarea"
              :rows="8"
              placeholder="请输入五点描述，每点一行"
            />
          </el-form-item>
          <el-form-item label="商品描述" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="6"
              placeholder="请输入商品描述"
            />
          </el-form-item>
          <el-form-item label="搜索关键词" prop="keywords">
            <el-input
              v-model="formData.keywords"
              type="textarea"
              :rows="4"
              placeholder="请输入搜索关键词，用逗号分隔"
            />
          </el-form-item>
        </el-tab-pane>

        <!-- 图片 -->
        <el-tab-pane label="图片" name="images">
          <el-form-item label="主图" prop="imageUrl">
            <el-input v-model="formData.imageUrl" placeholder="请输入主图URL" />
          </el-form-item>
          <el-form-item label="附图" prop="otherImages">
            <el-input
              v-model="formData.otherImages"
              type="textarea"
              :rows="6"
              placeholder="请输入附图URL，每行一个"
            />
          </el-form-item>
        </el-tab-pane>
      </el-tabs>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ProductApi } from '@/api/amazon/listing'
import { ShopApi } from '@/api/amazon/shop'

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const activeTab = ref('basic')

const shopList = ref([])

const formData = ref({
  id: undefined,
  shopId: undefined,
  asin: undefined,
  sku: undefined,
  title: undefined,
  brand: undefined,
  price: undefined,
  quantity: 0,
  bulletPoints: undefined,
  description: undefined,
  keywords: undefined,
  imageUrl: undefined,
  otherImages: undefined
})

const formRules = {
  shopId: [{ required: true, message: '店铺不能为空', trigger: 'change' }],
  asin: [{ required: true, message: 'ASIN不能为空', trigger: 'blur' }],
  sku: [{ required: true, message: 'SKU不能为空', trigger: 'blur' }],
  title: [{ required: true, message: '商品标题不能为空', trigger: 'blur' }],
  price: [{ required: true, message: '价格不能为空', trigger: 'blur' }]
}

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '添加商品' : '修改商品'
  formType.value = type
  formRef.value?.resetFields()
  activeTab.value = 'basic'

  // 加载店铺列表
  try {
    const data = await ShopApi.getShopPage({ pageNo: 1, pageSize: 100 })
    shopList.value = data.list
  } catch (error) {
    console.error('加载店铺列表失败', error)
  }

  if (id) {
    formLoading.value = true
    try {
      formData.value = await ProductApi.getProduct(id)
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
      await ProductApi.createProduct(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await ProductApi.updateProduct(formData.value)
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
