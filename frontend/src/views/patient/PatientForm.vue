<template>
  <div class="patient-form">
    <el-card>
      <template #header>
        <span>{{ isEdit ? '编辑患者' : '新增患者' }}</span>
      </template>

      <el-alert
        title="患者建档仅录入基础信息，诊断会在完成评估后由医生到诊断详情中确认。"
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      />
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="max-width: 800px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="患者姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入患者姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择性别" teleported style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                placeholder="请选择出生日期"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="form.age" :min="0" :max="150" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="紧急联系人" prop="emergencyContact">
              <el-input v-model="form.emergencyContact" placeholder="请输入紧急联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急联系电话" prop="emergencyPhone">
              <el-input v-model="form.emergencyPhone" placeholder="请输入紧急联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="科室" prop="departmentId">
              <el-select v-model="form.departmentId" placeholder="请选择科室" style="width: 100%" teleported>
                <el-option v-for="d in departmentList" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入备注"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading">保存</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { patientApi, departmentApi } from '../../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const isEdit = ref(false)
const departmentList = ref([])

const form = reactive({
  name: '',
  gender: '',
  birthDate: '',
  age: null,
  idCard: '',
  phone: '',
  address: '',
  emergencyContact: '',
  emergencyPhone: '',
  departmentId: null,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入患者姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  departmentId: [{ required: true, message: '请选择科室', trigger: 'change' }]
}

const loadDepartments = async () => {
  try {
    const res = await departmentApi.listAll()
    departmentList.value = res.data || []
  } catch (e) {
    ElMessage.error('加载科室列表失败')
  }
}

const handleSubmit = async () => {
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        if (isEdit.value) {
          await patientApi.updatePatient({ ...form, id: route.params.id })
          ElMessage.success('更新成功')
        } else {
          await patientApi.addPatient(form)
          ElMessage.success('添加成功')
        }
        router.push('/patient')
      } catch (error) {
        ElMessage.error('保存失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const handleCancel = () => {
  router.push('/patient')
}

const fetchData = async () => {
  if (route.params.id) {
    isEdit.value = true
    try {
      const res = await patientApi.getPatientForEdit(route.params.id)
      Object.assign(form, res.data)
    } catch (error) {
      ElMessage.error('获取患者信息失败')
    }
  }
}

onMounted(async () => {
  await loadDepartments()
  fetchData()
})
</script>

<style scoped>
.patient-form {
  padding: 20px;
}
</style>

