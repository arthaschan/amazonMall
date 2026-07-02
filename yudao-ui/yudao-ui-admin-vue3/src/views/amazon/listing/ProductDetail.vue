<template>
  <el-dialog v-model="dialogVisible" title="商品详情" width="900px">
    <el-tabs v-model="activeTab" v-loading="loading">
      <!-- 基本信息 -->
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ASIN">{{ data.asin }}</el-descriptions-item>
          <el-descriptions-item label="SKU">{{ data.sku }}</el-descriptions-item>
          <el-descriptions-item label="商品标题" :span="2">{{ data.title }}</el-descriptions-item>
          <el-descriptions-item label="品牌">{{ data.brand }}</el-descriptions-item>
          <el-descriptions-item label="店铺">{{ data.shopName }}</el-descriptions-item>
          <el-descriptions-item label="价格">${{ data.price }}</el-descriptions-item>
          <el-descriptions-item label="库存">{{ data.quantity }}</el-descriptions-item>
          <el-descriptions-item label="BSR排名">
            <span v-if="data.bsrRank">#{{ data.bsrRank }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="评分">
            <span v-if="data.rating">{{ data.rating }} ⭐</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="评论数">{{ data.reviewCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ dateFormatter(data.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ dateFormatter(data.updateTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 商品图片 -->
        <div class="mt-20px">
          <h4 class="font-bold mb-10px">商品图片</h4>
          <el-row :gutter="10">
            <el-col :span="6" v-if="data.imageUrl">
              <el-image
                :src="data.imageUrl"
                :preview-src-list="[data.imageUrl]"
                fit="cover"
                class="w-full h-200px"
              />
              <div class="text-center text-gray-500 mt-5px">主图</div>
            </el-col>
            <el-col :span="6" v-for="(img, index) in otherImages" :key="index">
              <el-image
                :src="img"
                :preview-src-list="otherImages"
                fit="cover"
                class="w-full h-200px"
              />
              <div class="text-center text-gray-500 mt-5px">附图 {{ index + 1 }}</div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 详情描述 -->
      <el-tab-pane label="详情描述" name="description">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="五点描述">
            <pre class="whitespace-pre-wrap">{{ data.bulletPoints }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="商品描述">
            <pre class="whitespace-pre-wrap">{{ data.description }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="搜索关键词">
            <el-tag
              v-for="(keyword, index) in keywordsList"
              :key="index"
              class="mr-5px mb-5px"
              type="info"
            >
              {{ keyword }}
            </el-tag>
            <span v-if="!keywordsList.length">暂无关键词</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- 版本历史 -->
      <el-tab-pane label="版本历史" name="versions">
        <el-table :data="versionList" v-loading="versionLoading">
          <el-table-column label="版本号" align="center" prop="version" width="100" />
          <el-table-column label="修改内容" align="left" prop="changeSummary" min-width="200" />
          <el-table-column label="修改人" align="center" prop="operator" width="120" />
          <el-table-column
            label="修改时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            width="180"
          />
          <el-table-column label="操作" align="center" width="120">
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="handleRollback(scope.row.id)"
                :disabled="scope.row.version === 1"
              >
                回滚
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { ProductApi, ListingVersionApi } from '@/api/amazon/listing'

const message = useMessage()

const dialogVisible = ref(false)
const loading = ref(false)
const activeTab = ref('basic')
const data = ref({})

const otherImages = computed(() => {
  if (!data.value.otherImages) return []
  return data.value.otherImages.split('\n').filter((img) => img.trim())
})

const keywordsList = computed(() => {
  if (!data.value.keywords) return []
  return data.value.keywords.split(',').map((k) => k.trim()).filter((k) => k)
})

/** 版本历史 */
const versionLoading = ref(false)
const versionList = ref([])

const loadVersions = async (productId: number) => {
  versionLoading.value = true
  try {
    versionList.value = await ListingVersionApi.getVersionList(productId)
  } finally {
    versionLoading.value = false
  }
}

const handleRollback = async (versionId: number) => {
  try {
    await message.confirm('确定要回滚到该版本吗？')
    await ListingVersionApi.rollbackToVersion(versionId)
    message.success('回滚成功')
    dialogVisible.value = false
  } catch {}
}

const open = async (id: number) => {
  dialogVisible.value = true
  activeTab.value = 'basic'
  loading.value = true
  try {
    data.value = await ProductApi.getProduct(id)
    loadVersions(id)
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
