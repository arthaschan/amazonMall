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
      <el-form-item label="ASIN" prop="asin">
        <el-input
          v-model="queryParams.asin"
          placeholder="请输入ASIN"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="SKU" prop="sku">
        <el-input
          v-model="queryParams.sku"
          placeholder="请输入SKU"
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
          v-hasPermi="['amazon:listing:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="danger"
          plain
          @click="handleBatchDelete"
          v-hasPermi="['amazon:listing:delete']"
          :disabled="!selectedIds.length"
        >
          <Icon icon="ep:delete" class="mr-5px" /> 批量删除
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="list"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column label="商品图片" align="center" prop="imageUrl" width="100">
        <template #default="scope">
          <el-image
            v-if="scope.row.imageUrl"
            :src="scope.row.imageUrl"
            :preview-src-list="[scope.row.imageUrl]"
            fit="cover"
            class="w-60px h-60px"
          />
          <span v-else>暂无图片</span>
        </template>
      </el-table-column>
      <el-table-column label="商品信息" align="left" min-width="250">
        <template #default="scope">
          <div class="font-bold">{{ scope.row.title }}</div>
          <div class="text-gray-500 text-12px mt-5px">
            ASIN: {{ scope.row.asin }} | SKU: {{ scope.row.sku }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="店铺" align="center" prop="shopName" width="120" />
      <el-table-column label="价格" align="center" prop="price" width="100">
        <template #default="scope">
          ${{ scope.row.price }}
        </template>
      </el-table-column>
      <el-table-column label="库存" align="center" prop="quantity" width="80" />
      <el-table-column label="BSR排名" align="center" prop="bsrRank" width="100">
        <template #default="scope">
          <span v-if="scope.row.bsrRank">#{{ scope.row.bsrRank }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column
        label="更新时间"
        align="center"
        prop="updateTime"
        :formatter="dateFormatter"
        width="180"
      />
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">
            详情
          </el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['amazon:listing:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['amazon:listing:delete']"
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
  <ProductForm ref="formRef" @success="getList" />

  <!-- 详情弹窗 -->
  <ProductDetail ref="detailRef" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { ProductApi } from '@/api/amazon/listing'
import { ShopApi } from '@/api/amazon/shop'
import ProductForm from './ProductForm.vue'
import ProductDetail from './ProductDetail.vue'

const message = useMessage()
const { t } = useI18n()

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
  asin: undefined,
  sku: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await ProductApi.getProductPage(queryParams)
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

/** 多选操作 */
const selectedIds = ref([])
const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((row) => row.id)
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 查看详情 */
const detailRef = ref()
const openDetail = (id: number) => {
  detailRef.value.open(id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await ProductApi.deleteProduct(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 批量删除 */
const handleBatchDelete = async () => {
  try {
    await message.confirm(`确定要删除选中的 ${selectedIds.value.length} 个商品吗？`)
    await ProductApi.batchDeleteProducts(selectedIds.value)
    message.success(t('common.delSuccess'))
    selectedIds.value = []
    await getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  loadShopList()
  getList()
})
</script>
