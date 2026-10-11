// 与 H5 端保持一致的字典与格式化（业务语义统一）
export const ORDER_STATUS = {
  1: { label: '待支付', type: 'info' },
  2: { label: '待接单', type: 'warning' },
  3: { label: '进行中', type: 'primary' },
  4: { label: '已交付', type: 'primary' },
  5: { label: '已完成', type: 'success' },
  6: { label: '已取消', type: 'danger' },
  7: { label: '已超时', type: 'danger' },
  8: { label: '返修中', type: 'warning' },
  9: { label: '仲裁中', type: 'danger' },
}

export const BOOKING_STATUS = {
  0: { label: '待确认', type: 'warning' },
  1: { label: '已确认', type: 'primary' },
  2: { label: '已完成', type: 'success' },
  3: { label: '已拒绝', type: 'danger' },
  4: { label: '已取消', type: 'info' },
}

export const PORTFOLIO_STATUS = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已驳回', type: 'danger' },
}

export const TX_TYPE = { 1: '技能服务收入', 2: '支付支出', 3: '充值', 4: '提现', 5: '退款', 6: '违约金' }
export const WD_STATUS = ['待处理', '已打款', '已驳回']
export const LEVEL_NAMES = ['', '普通', '铜牌', '银牌', '金牌']

export function fmtMoney(v) {
  return '￥' + Number(v || 0).toFixed(2)
}

export function fmtTime(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 16)
}

export function relTime(t) {
  if (!t) return ''
  const d = new Date(String(t).replace(' ', 'T'))
  const diff = Math.floor((Date.now() - d.getTime()) / 60000)
  if (diff < 1) return '刚刚'
  if (diff < 60) return diff + ' 分钟前'
  if (diff < 1440) return Math.floor(diff / 60) + ' 小时前'
  if (diff < 2880) return '昨天 ' + String(t).slice(11, 16)
  return String(t).slice(5, 16)
}

export function modeText(m) {
  return m === 1 ? '线上交付' : '线下进行'
}
