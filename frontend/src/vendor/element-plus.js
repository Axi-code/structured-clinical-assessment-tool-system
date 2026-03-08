import * as ElementPlusModule from '../../node_modules/element-plus/es/index.mjs'

const ElementPlus = ElementPlusModule.default ?? ElementPlusModule
const { ElMessage, ElMessageBox } = ElementPlusModule

export default ElementPlus
export { ElMessage, ElMessageBox }

